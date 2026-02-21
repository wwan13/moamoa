package server.messaging.read

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.springframework.data.redis.connection.stream.MapRecord
import org.springframework.data.redis.connection.stream.RecordId
import org.springframework.data.redis.core.StreamOperations
import org.springframework.stereotype.Component
import server.messaging.StreamEventHandlers
import server.shared.messaging.SubscriptionDefinition

@Component
internal class StreamMessageProcessor(
    private val handlers: StreamEventHandlers,
    private val objectMapper: ObjectMapper,
) {
    private val logger = KotlinLogging.logger {}

    fun subscriptions(): List<SubscriptionDefinition> = handlers.subscriptions()

    suspend fun handleRecords(
        subscription: SubscriptionDefinition,
        ops: StreamOperations<String, String, String>,
        records: List<MapRecord<String, String, String>>,
        launchScope: CoroutineScope,
    ) {
        for (record in records) {
            handleRecord(subscription, ops, record, launchScope)
        }
    }

    private suspend fun handleRecord(
        subscription: SubscriptionDefinition,
        ops: StreamOperations<String, String, String>,
        record: MapRecord<String, String, String>,
        launchScope: CoroutineScope,
    ) {
        val type = record.value["type"]
        val payloadJson = record.value["payload"]

        if (type.isNullOrBlank() || payloadJson.isNullOrBlank()) {
            ack(ops, subscription, record.id)
            return
        }

        val messageHandler = handlers.find<Any>(subscription, type)
        if (messageHandler == null) {
            logger.warn {
                "No handler. channelKey=${subscription.channel} consumerGroup=${subscription.consumerGroup} type=$type"
            }
            ack(ops, subscription, record.id)
            return
        }

        val payload = runCatching {
            objectMapper.readValue(payloadJson, messageHandler.payloadClass)
        }.getOrElse { e ->
            logger.warn(e) {
                "payload deserialize failed. channelKey=${subscription.channel} consumerGroup=${subscription.consumerGroup} " +
                    "type=$type id=${record.id}"
            }
            if (subscription.ackOnFailure) ack(ops, subscription, record.id)
            return
        }

        if (subscription.processSequentially) {
            try {
                messageHandler.handler(payload)
                ack(ops, subscription, record.id)
            } catch (e: Exception) {
                logger.warn(e) {
                    "Handler failed. channelKey=${subscription.channel} consumerGroup=${subscription.consumerGroup} " +
                        "type=$type id=${record.id}"
                }
                if (subscription.ackOnFailure) ack(ops, subscription, record.id)
            }
            return
        }

        launchScope.launch {
            try {
                messageHandler.handler(payload)
                ack(ops, subscription, record.id)
            } catch (e: Exception) {
                logger.warn(e) {
                    "Handler failed(non-sequential). channelKey=${subscription.channel} consumerGroup=${subscription.consumerGroup} " +
                        "type=$type id=${record.id}"
                }
                if (subscription.ackOnFailure) ack(ops, subscription, record.id)
            }
        }
    }

    private suspend fun ack(
        ops: StreamOperations<String, String, String>,
        subscription: SubscriptionDefinition,
        recordId: RecordId
    ) = withContext(Dispatchers.IO) {
        ops.acknowledge(subscription.channel.key, subscription.consumerGroup, recordId)
    }
}

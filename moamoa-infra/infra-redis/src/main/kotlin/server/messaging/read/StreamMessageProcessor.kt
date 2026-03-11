package server.messaging.read

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.springframework.data.redis.connection.stream.MapRecord
import org.springframework.data.redis.connection.stream.RecordId
import org.springframework.data.redis.core.StreamOperations
import org.springframework.stereotype.Component
import server.messaging.MessageHandlerInvoker
import server.messaging.StreamEventHandlers
import server.messaging.annotation.EventStream

@Component
internal class StreamMessageProcessor(
    private val handlers: StreamEventHandlers,
    private val objectMapper: ObjectMapper,
    private val messageHandlerInvoker: MessageHandlerInvoker,
) {
    private val logger = KotlinLogging.logger {}

    fun streams(): List<EventStream> = handlers.streams()

    fun handleRecords(
        stream: EventStream,
        ops: StreamOperations<String, String, String>,
        records: List<MapRecord<String, String, String>>,
        launchScope: CoroutineScope,
    ) {
        for (record in records) {
            handleRecord(stream, ops, record, launchScope)
        }
    }

    private fun handleRecord(
        stream: EventStream,
        ops: StreamOperations<String, String, String>,
        record: MapRecord<String, String, String>,
        launchScope: CoroutineScope,
    ) {
        val type = record.value["type"]
        val payloadJson = record.value["payload"]
        val eventId = record.value["eventId"]

        if (type.isNullOrBlank() || payloadJson.isNullOrBlank()) {
            ack(ops, stream, record.id)
            return
        }

        val messageHandler = handlers.find<Any>(stream, type)
        if (messageHandler == null) {
            logger.warn {
                "No handler. channelKey=${stream.channel.key} consumerGroup=${stream.consumerGroup} type=$type"
            }
            ack(ops, stream, record.id)
            return
        }

        val payload = runCatching {
            objectMapper.readValue(payloadJson, messageHandler.payloadClass)
        }.getOrElse { e ->
            logger.warn(e) {
                "payload deserialize failed. channelKey=${stream.channel.key} consumerGroup=${stream.consumerGroup} " +
                    "type=$type id=${record.id}"
            }
            if (stream.ackOnFailure) ack(ops, stream, record.id)
            return
        }

        if (stream.processSequentially) {
            try {
                messageHandlerInvoker.invoke(eventId, type, payload, messageHandler.handler)
                ack(ops, stream, record.id)
            } catch (e: Exception) {
                logger.warn(e) {
                    "Handler failed. channelKey=${stream.channel.key} consumerGroup=${stream.consumerGroup} " +
                        "type=$type id=${record.id}"
                }
                if (stream.ackOnFailure) ack(ops, stream, record.id)
            }
            return
        }

        launchScope.launch {
            try {
                messageHandlerInvoker.invoke(eventId, type, payload, messageHandler.handler)
                ack(ops, stream, record.id)
            } catch (e: Exception) {
                logger.warn(e) {
                    "Handler failed(non-sequential). channelKey=${stream.channel.key} consumerGroup=${stream.consumerGroup} " +
                        "type=$type id=${record.id}"
                }
                if (stream.ackOnFailure) ack(ops, stream, record.id)
            }
        }
    }

    private fun ack(
        ops: StreamOperations<String, String, String>,
        stream: EventStream,
        recordId: RecordId
    ) {
        ops.acknowledge(stream.channel.key, stream.consumerGroup, recordId)
    }
}

package server.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.RedisSystemException
import org.springframework.data.redis.connection.stream.Consumer
import org.springframework.data.redis.connection.stream.MapRecord
import org.springframework.data.redis.connection.stream.ReadOffset
import org.springframework.data.redis.connection.stream.RecordId
import org.springframework.data.redis.connection.stream.StreamOffset
import org.springframework.data.redis.connection.stream.StreamReadOptions
import org.springframework.data.redis.core.StreamOperations
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import server.shared.messaging.SubscriptionDefinition
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Component
internal class SubscriptionReader(
    @param:Qualifier("streamStringRedisTemplate")
    private val redis: StringRedisTemplate,
    private val handlers: SubscriptionEventHandlers,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val jobs = ConcurrentHashMap<String, Job>()

    private data class EnsureKey(val channelKey: String, val consumerGroup: String)

    private val ensured = ConcurrentHashMap.newKeySet<EnsureKey>()

    @PostConstruct
    fun start() {
        handlers.subscriptions().forEach { subscription ->
            startLoopIfAbsent(subscription)
        }
    }

    @PreDestroy
    fun stop() {
        jobs.values.forEach { it.cancel() }
        scope.cancel()
    }

    private fun startLoopIfAbsent(subscription: SubscriptionDefinition) {
        val jobKey = "${subscription.channel}::${subscription.consumerGroup}"
        jobs.computeIfAbsent(jobKey) {
            scope.launch { loopForSubscription(subscription) }
        }
    }

    private suspend fun loopForSubscription(subscription: SubscriptionDefinition) {
        val ops = redis.opsForStream<String, String>()
        val consumerName = "worker-${subscription.channel}-${subscription.consumerGroup}-${UUID.randomUUID()}"
        val consumer = Consumer.from(subscription.consumerGroup, consumerName)

        val options = StreamReadOptions.empty()
            .block(Duration.ofSeconds(2))
            .count(subscription.batchSize.coerceAtLeast(1).toLong())

        ensureGroupOnce(subscription, ops)

        while (currentCoroutineContext().isActive) {
            val records = try {
                withContext(Dispatchers.IO) {
                    ops.read(
                        consumer,
                        options,
                        StreamOffset.create(subscription.channel.key, ReadOffset.lastConsumed())
                    )
                } ?: emptyList()
            } catch (e: RedisSystemException) {
                break
            } catch (e: Exception) {
                if (e.message?.contains("Connection closed") == true) break
                log.warn(
                    "read failed. channelKey={} consumerGroup={}",
                    subscription.channel,
                    subscription.consumerGroup,
                    e
                )
                delay(500)
                continue
            }

            for (record in records) {
                handleRecord(subscription, ops, record)
            }

            if (records.isEmpty()) delay(50)
        }
    }

    private suspend fun handleRecord(
        subscription: SubscriptionDefinition,
        ops: StreamOperations<String, String, String>,
        record: MapRecord<String, String, String>,
    ) {
        val type = record.value["type"]
        val payloadJson = record.value["payload"]

        if (type.isNullOrBlank() || payloadJson.isNullOrBlank()) {
            ack(ops, subscription, record.id)
            return
        }

        val messageHandler = handlers.find<Any>(subscription, type)
        if (messageHandler == null) {
            log.warn(
                "No handler. channelKey={} consumerGroup={} type={}",
                subscription.channel,
                subscription.consumerGroup,
                type
            )
            ack(ops, subscription, record.id)
            return
        }

        val payload = runCatching {
            objectMapper.readValue(payloadJson, messageHandler.payloadClass)
        }.getOrElse { e ->
            log.warn(
                "payload deserialize failed. channelKey={} consumerGroup={} type={} id={}",
                subscription.channel,
                subscription.consumerGroup,
                type,
                record.id,
                e
            )
            if (subscription.ackOnFailure) ack(ops, subscription, record.id)
            return
        }

        if (subscription.processSequentially) {
            try {
                messageHandler.handler(payload)
                ack(ops, subscription, record.id)
            } catch (e: Exception) {
                log.warn(
                    "Handler failed. channelKey={} consumerGroup={} type={} id={}",
                    subscription.channel,
                    subscription.consumerGroup,
                    type,
                    record.id,
                    e
                )
                if (subscription.ackOnFailure) ack(ops, subscription, record.id)
            }
            return
        }

        scope.launch {
            try {
                messageHandler.handler(payload)
                ack(ops, subscription, record.id)
            } catch (e: Exception) {
                log.warn(
                    "Handler failed(non-sequential). channelKey={} consumerGroup={} type={} id={}",
                    subscription.channel,
                    subscription.consumerGroup,
                    type,
                    record.id,
                    e
                )
                if (subscription.ackOnFailure) ack(ops, subscription, record.id)
            }
        }
    }

    private suspend fun ensureGroupOnce(
        subscription: SubscriptionDefinition,
        ops: StreamOperations<String, String, String>
    ) = withContext(Dispatchers.IO) {
        if (!ensured.add(EnsureKey(subscription.channel.key, subscription.consumerGroup))) return@withContext
        try {
            ops.createGroup(subscription.channel.key, ReadOffset.from("0-0"), subscription.consumerGroup)
        } catch (_: Exception) {
            // BUSYGROUP 등은 정상 취급
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

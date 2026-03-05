package server.messaging.retry

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Range
import org.springframework.data.redis.connection.stream.StreamRecords
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import server.messaging.MessageChannel
import server.messaging.StreamEventHandlers
import server.messaging.SubscriptionDefinition
import server.messaging.health.RedisHealthStateManager
import java.time.Duration
import io.github.oshai.kotlinlogging.KotlinLogging.logger as kLogger

@Component
internal class StreamRetryProcessor(
    @param:Qualifier("streamStringRedisTemplate")
    private val redisTemplate: StringRedisTemplate,
    @param:Qualifier("defaultDlqTopic")
    private val defaultDlqTopic: MessageChannel,
    private val objectMapper: ObjectMapper,
    private val eventHandlers: StreamEventHandlers,
    private val healthStateManager: RedisHealthStateManager,
) {
    private val log = kLogger {}

    private val streamOps = redisTemplate.opsForStream<String, String>()

    private val consumerName = "subscription-retrier"
    private val minIdle = Duration.ofMinutes(5)
    private val fetchCountPerSubscription = 100L
    private val maxRetryCount = 3L

    fun processOnce(): Boolean {
        if (healthStateManager.isDegraded()) {
            val recovered = healthStateManager.tryRecover()
            if (!recovered) return false
        }

        for (subscription in eventHandlers.subscriptions()) {
            val executed = reclaimSubscription(subscription)
            if (!executed) return false
            if (healthStateManager.isDegraded()) return false
        }
        return true
    }

    private fun reclaimSubscription(subscription: SubscriptionDefinition): Boolean {
        val channelKey = subscription.channel.key
        val consumerGroup = subscription.consumerGroup

        val result = healthStateManager.runSafe {
            val pending = streamOps.pending(channelKey, consumerGroup, Range.unbounded<String>(), fetchCountPerSubscription)
            val pendingMessages = pending.toList()
            if (pendingMessages.isEmpty()) return@runSafe

            val deliveryCountById = pendingMessages.associate { it.id to it.totalDeliveryCount }
            val targetIds = pendingMessages
                .asSequence()
                .filter { it.elapsedTimeSinceLastDelivery >= minIdle }
                .map { it.id }
                .take(fetchCountPerSubscription.toInt())
                .toList()

            if (targetIds.isEmpty()) return@runSafe

            val claimed = streamOps.claim(channelKey, consumerGroup, consumerName, minIdle, *targetIds.toTypedArray())

            for (record in claimed) {
                val id = record.id
                val idValue = id.value
                val type = record.value["type"]
                val payloadJson = record.value["payload"]
                val deliveryCount = deliveryCountById[id] ?: 0L

                if (type.isNullOrBlank() || payloadJson.isNullOrBlank()) {
                    log.warn { "잘못된 메시지 포맷. channelKey=$channelKey, consumerGroup=$consumerGroup, id=$idValue, value=${record.value}" }
                    streamOps.acknowledge(channelKey, consumerGroup, record.id)
                    continue
                }

                if (deliveryCount > maxRetryCount) {
                    moveToDlq(channelKey, consumerGroup, idValue, deliveryCount, type, payloadJson)
                    streamOps.acknowledge(channelKey, consumerGroup, record.id)
                    continue
                }

                val messageHandler = eventHandlers.find<Any>(subscription, type)
                if (messageHandler == null) {
                    log.warn { "핸들러 없음. channelKey=$channelKey, consumerGroup=$consumerGroup, type=$type, id=$idValue" }
                    streamOps.acknowledge(channelKey, consumerGroup, record.id)
                    continue
                }

                try {
                    val payload = objectMapper.readValue(payloadJson, messageHandler.payloadClass)
                    messageHandler.handler(payload)
                    streamOps.acknowledge(channelKey, consumerGroup, record.id)
                } catch (e: Exception) {
                    log.warn(e) { "재처리 실패. channelKey=$channelKey, consumerGroup=$consumerGroup, type=$type, id=$idValue" }
                }
            }
        }

        return result.isSuccess
    }

    private fun moveToDlq(
        channelKey: String,
        consumerGroup: String,
        sourceId: String,
        deliveryCount: Long,
        type: String,
        payloadJson: String,
    ) {
        val dlqRecord = StreamRecords
            .mapBacked<String, String, String>(
                mapOf(
                    "type" to type,
                    "payload" to payloadJson,
                    "sourceChannel" to channelKey,
                    "sourceGroup" to consumerGroup,
                    "sourceId" to sourceId,
                    "deliveryCount" to deliveryCount.toString(),
                ),
            )
            .withStreamKey(defaultDlqTopic.key)

        runCatching {
            streamOps.add(dlqRecord)
        }.onSuccess {
            log.warn {
                "메시지를 DLQ로 이동. channelKey=$channelKey, consumerGroup=$consumerGroup, id=$sourceId, " +
                    "deliveryCount=$deliveryCount, dlq=${defaultDlqTopic.key}"
            }
        }.onFailure { e ->
            log.warn(e) {
                "DLQ 이동 실패. channelKey=$channelKey, consumerGroup=$consumerGroup, id=$sourceId, " +
                    "deliveryCount=$deliveryCount, dlq=${defaultDlqTopic.key}"
            }
            throw e
        }
    }
}

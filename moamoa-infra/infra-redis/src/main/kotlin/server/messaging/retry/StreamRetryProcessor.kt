package server.messaging.retry

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Range
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Component
import server.messaging.health.RedisHealthStateManager
import server.messaging.StreamEventHandlers
import server.shared.messaging.SubscriptionDefinition
import java.time.Duration

@Component
internal class StreamRetryProcessor(
    @param:Qualifier("streamReactiveRedisTemplate")
    private val redisTemplate: ReactiveRedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
    private val eventHandlers: StreamEventHandlers,
    private val healthStateManager: RedisHealthStateManager,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val streamOps = redisTemplate.opsForStream<String, String>()

    private val consumerName = "subscription-retrier"
    private val minIdle = Duration.ofMinutes(5)
    private val fetchCountPerSubscription = 100L

    suspend fun processOnce(): Boolean {
        for (subscription in eventHandlers.subscriptions()) {
            val executed = reclaimSubscription(subscription)
            if (!executed) return false
            if (healthStateManager.isDegraded()) return false
        }
        return true
    }

    private suspend fun reclaimSubscription(subscription: SubscriptionDefinition): Boolean {
        val channelKey = subscription.channel.key
        val consumerGroup = subscription.consumerGroup

        val result = healthStateManager.runSafe {
            val pending = streamOps
                .pending(channelKey, consumerGroup, Range.unbounded<String>(), fetchCountPerSubscription)
                .awaitSingle()

            val pendingMessages = pending.toList()

            if (pendingMessages.isEmpty()) return@runSafe

            val targetIds = pendingMessages
                .asSequence()
                .filter { it.elapsedTimeSinceLastDelivery >= minIdle }
                .map { it.id }
                .take(fetchCountPerSubscription.toInt())
                .toList()

            if (targetIds.isEmpty()) return@runSafe

            val claimed = streamOps
                .claim(channelKey, consumerGroup, consumerName, minIdle, *targetIds.toTypedArray())
                .collectList()
                .awaitSingle()

            for (record in claimed) {
                val id = record.id.value
                val type = record.value["type"]
                val payloadJson = record.value["payload"]

                if (type.isNullOrBlank() || payloadJson.isNullOrBlank()) {
                    log.warn(
                        "잘못된 메시지 포맷. channelKey={}, consumerGroup={}, id={}, value={}",
                        channelKey,
                        consumerGroup,
                        id,
                        record.value
                    )
                    streamOps.acknowledge(channelKey, consumerGroup, record.id).awaitFirstOrNull()
                    continue
                }

                val messageHandler = eventHandlers.find<Any>(subscription, type)
                if (messageHandler == null) {
                    log.warn(
                        "핸들러 없음. channelKey={}, consumerGroup={}, type={}, id={}",
                        channelKey,
                        consumerGroup,
                        type,
                        id
                    )
                    streamOps.acknowledge(channelKey, consumerGroup, record.id).awaitFirstOrNull()
                    continue
                }

                try {
                    val payload = objectMapper.readValue(payloadJson, messageHandler.payloadClass)
                    messageHandler.handler(payload)
                    streamOps.acknowledge(channelKey, consumerGroup, record.id).awaitFirstOrNull()
                } catch (e: Exception) {
                    log.warn(
                        "재처리 실패. channelKey={}, consumerGroup={}, type={}, id={}",
                        channelKey,
                        consumerGroup,
                        type,
                        id,
                        e
                    )
                }
            }
        }

        return result.isSuccess
    }
}

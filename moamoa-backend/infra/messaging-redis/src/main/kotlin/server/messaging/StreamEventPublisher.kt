package server.messaging

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.connection.stream.StreamRecords
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import server.messaging.health.RedisHealthStateManager
import java.time.Instant

@Component
internal class StreamEventPublisher(
    @param:Qualifier("streamStringRedisTemplate")
    private val redis: StringRedisTemplate,
    private val healthStateManager: RedisHealthStateManager,
) : EventPublisher {
    override fun publish(
        channel: String,
        type: String,
        payloadJson: String,
        eventId: String
    ) {
        if (eventId.isBlank()) {
            throw IllegalArgumentException("eventId must not be blank")
        }

        if (healthStateManager.isDegraded()) {
            val recovered = healthStateManager.tryRecover()
            if (!recovered) {
                throw IllegalStateException("messaging broker is degraded")
            }
        }

        val fields = mapOf(
            "type" to type,
            "eventId" to eventId,
            "occurredAt" to Instant.now().toString(),
            "payload" to payloadJson,
        )

        val record = StreamRecords
            .mapBacked<String, String, String>(fields)
            .withStreamKey(channel)

        healthStateManager.runSafe {
            redis.opsForStream<String, String>().add(record)
                ?: throw IllegalStateException("Redis XADD returned null")
        }.getOrElse { e ->
            throw IllegalStateException("messaging broker is unavailable", e)
        }
    }
}

package server.messaging

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.connection.stream.StreamRecords
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Instant

@Component
internal class StreamEventPublisher(
    @param:Qualifier("streamStringRedisTemplate")
    private val redis: StringRedisTemplate,
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

        val fields = mapOf(
            "type" to type,
            "eventId" to eventId,
            "occurredAt" to Instant.now().toString(),
            "payload" to payloadJson,
        )

        val record = StreamRecords
            .mapBacked<String, String, String>(fields)
            .withStreamKey(channel)

        redis.opsForStream<String, String>().add(record)
            ?: throw IllegalStateException("Redis XADD returned null")
    }
}

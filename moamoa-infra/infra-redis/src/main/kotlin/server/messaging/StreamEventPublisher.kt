package server.messaging

import io.github.oshai.kotlinlogging.KotlinLogging.logger as kLogger
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
    private val log = kLogger {}

    override fun publish(
        channel: String,
        type: String,
        payloadJson: String,
        eventId: String
    ) {
        if (eventId.isBlank()) {
            log.warn { "Skip publish. eventId is blank: streamKey=$channel type=$type" }
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

        try {
            redis.opsForStream<String, String>().add(record)
                ?: throw IllegalStateException("Redis XADD returned null")
        } catch (e: Exception) {
            log.warn(e) { "Redis XADD failed: streamKey=$channel type=$type eventId=$eventId" }
            throw e
        }
    }
}

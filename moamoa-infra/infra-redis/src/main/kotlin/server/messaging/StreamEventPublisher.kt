package server.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging.logger as kLogger
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.connection.stream.StreamRecords
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import server.messaging.EventPublisher
import server.messaging.MessageChannel
import java.time.Instant
import java.util.UUID

@Component
internal class StreamEventPublisher(
    @param:Qualifier("streamStringRedisTemplate")
    private val redis: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) : EventPublisher {
    private val log = kLogger {}

    override fun publish(channel: MessageChannel, payload: Any) {
        publish(channel.key, payload::class.simpleName!!, objectMapper.writeValueAsString(payload))
    }

    override fun publish(channel: String, type: String, payloadJson: String) {
        val fields = mapOf(
            "type" to type,
            "eventId" to UUID.randomUUID().toString(),
            "occurredAt" to Instant.now().toString(),
            "payload" to payloadJson,
        )

        val record = StreamRecords
            .mapBacked<String, String, String>(fields)
            .withStreamKey(channel)

        try {
            val recordId = redis.opsForStream<String, String>().add(record)
            if (recordId == null) {
                throw IllegalStateException("Redis XADD returned null")
            }
        } catch (e: Exception) {
            log.warn("Redis XADD failed: streamKey={}", channel, e)
        }
    }
}

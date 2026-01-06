package server.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.data.redis.connection.stream.RecordId
import org.springframework.data.redis.connection.stream.StreamRecords
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID


@Component
class StreamEventPublisher(
    private val redis: ReactiveRedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun publish(topic: StreamTopic, payload: Any) {
        publishMono(topic.key, payload)
            .doOnError { e -> log.warn("Redis XADD failed: streamKey={}", topic.key, e) }
            .onErrorResume { Mono.empty() }
            .subscribe()
    }

    private fun publishMono(streamKey: String, payload: Any): Mono<RecordId> {
        val fields: Map<String, String> = mapOf(
            "type" to (payload::class.simpleName ?: payload::class.qualifiedName ?: "UnknownEvent"),
            "eventId" to UUID.randomUUID().toString(),
            "occurredAt" to Instant.now().toString(),
            "payload" to objectMapper.writeValueAsString(payload),
        )

        val record = StreamRecords
            .mapBacked<String, String, String>(fields)
            .withStreamKey(streamKey)

        return redis.opsForStream<String, String>()
            .add(record)
            .switchIfEmpty(Mono.error(IllegalStateException("Redis XADD returned empty")))
    }
}

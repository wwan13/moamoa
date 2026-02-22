package server.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging.logger as kLogger
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.connection.stream.RecordId
import org.springframework.data.redis.connection.stream.StreamRecords
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import server.shared.messaging.EventPublisher
import server.shared.messaging.MessageChannel
import java.time.Instant
import java.util.UUID

@Component
internal class StreamEventPublisher(
    @param:Qualifier("streamReactiveRedisTemplate")
    private val redis: ReactiveRedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) : EventPublisher {
    private val log = kLogger {}

    override fun publish(channel: MessageChannel, payload: Any) {
        publishMono(channel.key, payload::class.simpleName!!, objectMapper.writeValueAsString(payload))
            .doOnError { e -> log.warn("Redis XADD failed: streamKey={}", channel.key, e) }
            .onErrorResume { Mono.empty() }
            .subscribe()
    }

    override fun publish(channel: String, type: String, payloadJson: String) {
        publishMono(channel, type, payloadJson)
            .doOnError { e -> log.warn("Redis XADD failed: streamKey={}", channel, e) }
            .onErrorResume { Mono.empty() }
            .subscribe()
    }

    private fun publishMono(
        channelKey: String,
        type: String,
        payloadJson: String,
    ): Mono<RecordId> {
        val fields: Map<String, String> = mapOf(
            "type" to type,
            "eventId" to UUID.randomUUID().toString(),
            "occurredAt" to Instant.now().toString(),
            "payload" to payloadJson,
        )

        val record = StreamRecords
            .mapBacked<String, String, String>(fields)
            .withStreamKey(channelKey)

        return redis.opsForStream<String, String>()
            .add(record)
            .switchIfEmpty(Mono.error(IllegalStateException("Redis XADD returned empty")))
    }
}

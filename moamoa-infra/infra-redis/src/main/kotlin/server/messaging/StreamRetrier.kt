package server.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Range
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

@Component
internal class StreamRetrier(
    private val redisTemplate: ReactiveRedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
    private val eventHandlers: StreamEventHandlers,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val running = AtomicBoolean(false)

    private val streamOps = redisTemplate.opsForStream<String, String>()

    private val consumerName = "stream-retrier"
    private val minIdle = Duration.ofMinutes(5)
    private val fetchCountPerStream = 100L

    suspend fun runOnce() {
        if (!running.compareAndSet(false, true)) return

        try {
            for (stream in eventHandlers.streams()) {
                reclaimStream(stream)
            }
        } finally {
            running.set(false)
        }
    }

    private suspend fun reclaimStream(stream: StreamDefinition) {
        val streamKey = stream.topic.key
        val group = stream.group

        val pending = streamOps
            .pending(streamKey, group, Range.unbounded<String>(), fetchCountPerStream)
            .awaitSingle()

        val pendingMessages = pending.toList()

        if (pendingMessages.isEmpty()) return

        val targetIds = pendingMessages
            .asSequence()
            .filter { it.elapsedTimeSinceLastDelivery >= minIdle }
            .map { it.id }
            .take(fetchCountPerStream.toInt())
            .toList()

        if (targetIds.isEmpty()) return

        val claimed = streamOps
            .claim(streamKey, group, consumerName, minIdle, *targetIds.toTypedArray())
            .collectList()
            .awaitSingle()

        for (record in claimed) {
            val id = record.id.value
            val type = record.value["type"]
            val payloadJson = record.value["payload"]

            if (type.isNullOrBlank() || payloadJson.isNullOrBlank()) {
                log.warn("잘못된 메시지 포맷. streamKey={}, group={}, id={}, value={}", streamKey, group, id, record.value)
                streamOps.acknowledge(streamKey, group, record.id).awaitFirstOrNull()
                continue
            }

            val handler = eventHandlers.find<Any>(stream, type)
            if (handler == null) {
                log.warn("핸들러 없음. streamKey={}, group={}, type={}, id={}", streamKey, group, type, id)
                streamOps.acknowledge(streamKey, group, record.id).awaitFirstOrNull()
                continue
            }

            try {
                val payload = objectMapper.readValue(payloadJson, handler.payloadClass)
                handler.handler(payload)
                streamOps.acknowledge(streamKey, group, record.id).awaitFirstOrNull()
            } catch (e: Exception) {
                log.warn("재처리 실패. streamKey={}, group={}, type={}, id={}", streamKey, group, type, id, e)
            }
        }
    }
}
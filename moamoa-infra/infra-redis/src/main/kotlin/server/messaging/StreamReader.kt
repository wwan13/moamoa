package server.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.data.redis.RedisSystemException
import org.springframework.data.redis.connection.stream.*
import org.springframework.data.redis.core.StreamOperations
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Component
internal class StreamReader(
    private val redis: StringRedisTemplate,
    private val handlers: StreamEventHandlers,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val group = "moamoa"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val jobs = ConcurrentHashMap<String, Job>()

    @PostConstruct
    fun start() {
        handlers.streamKeys().forEach { streamKey ->
            startLoopIfAbsent(streamKey)
        }
    }

    @PreDestroy
    fun stop() {
        jobs.values.forEach { it.cancel() }
        scope.cancel()
    }

    private fun startLoopIfAbsent(streamKey: String) {
        jobs.computeIfAbsent(streamKey) {
            scope.launch {
                loopForStream(streamKey)
            }
        }
    }

    private suspend fun loopForStream(streamKey: String) {
        val ops = redis.opsForStream<String, String>()
        val consumerName = "worker-${streamKey}-${UUID.randomUUID()}"
        val consumer = Consumer.from(group, consumerName)

        val options = StreamReadOptions.empty()
            .block(Duration.ofSeconds(2))
            .count(50)

        ensureGroupOnce(streamKey, ops)

        while (currentCoroutineContext().isActive) {
            val records = try {
                withContext(Dispatchers.IO) {
                    ops.read(
                        consumer,
                        options,
                        StreamOffset.create(streamKey, ReadOffset.lastConsumed())
                    )
                } ?: emptyList()
            } catch (e: RedisSystemException) {
                break
            } catch (e: Exception) {
                if (e.message?.contains("Connection closed") == true) break
                log.warn("read failed. streamKey={}", streamKey, e)
                delay(500)
                continue
            }

            for (record in records) {
                val type = record.value["type"]
                val payloadJson = record.value["payload"]

                if (type.isNullOrBlank() || payloadJson.isNullOrBlank()) {
                    ack(ops, streamKey, record.id)
                    continue
                }

                val handler = handlers.find<Any>(streamKey, type)
                if (handler == null) {
                    log.warn("No handler. streamKey={} type={}", streamKey, type)
                    ack(ops, streamKey, record.id)
                    continue
                }

                try {
                    val event = objectMapper.readValue(payloadJson, handler.payloadClass)
                    handler.handler(event)
                    ack(ops, streamKey, record.id)
                } catch (e: Exception) {
                    log.warn(
                        "Handler failed. streamKey={} type={} id={}",
                        streamKey, type, record.id, e
                    )
                }
            }

            if (records.isEmpty()) delay(50)
        }
    }

    private suspend fun ensureGroupOnce(
        streamKey: String,
        ops: StreamOperations<String, String, String>
    ) = withContext(Dispatchers.IO) {
        try {
            ops.createGroup(streamKey, ReadOffset.from("0-0"), group)
        } catch (e: Exception) {
            // BUSYGROUP → 정상
        }
    }

    private suspend fun ack(
        ops: StreamOperations<String, String, String>,
        streamKey: String,
        recordId: RecordId
    ) = withContext(Dispatchers.IO) {
        ops.acknowledge(streamKey, group, recordId)
    }
}
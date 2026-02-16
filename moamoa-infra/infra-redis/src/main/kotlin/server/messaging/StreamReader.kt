package server.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.RedisSystemException
import org.springframework.data.redis.connection.stream.*
import org.springframework.data.redis.core.StreamOperations
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Component
internal class StreamReader(
    @param:Qualifier("streamStringRedisTemplate")
    private val redis: StringRedisTemplate,
    private val handlers: StreamEventHandlers,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val jobs = ConcurrentHashMap<String, Job>()

    private data class EnsureKey(val streamKey: String, val group: String)

    private val ensured = ConcurrentHashMap.newKeySet<EnsureKey>()

    @PostConstruct
    fun start() {
        handlers.streams().forEach { stream ->
            startLoopIfAbsent(stream)
        }
    }

    @PreDestroy
    fun stop() {
        jobs.values.forEach { it.cancel() }
        scope.cancel()
    }

    private fun startLoopIfAbsent(stream: StreamDefinition) {
        val jobKey = "${stream.topic}::${stream.group}"
        jobs.computeIfAbsent(jobKey) {
            scope.launch { loopForStream(stream) }
        }
    }

    private suspend fun loopForStream(stream: StreamDefinition) {
        val ops = redis.opsForStream<String, String>()
        val consumerName = "worker-${stream.topic}-${stream.group}-${UUID.randomUUID()}"
        val consumer = Consumer.from(stream.group, consumerName)

        val options = StreamReadOptions.empty()
            .block(Duration.ofSeconds(2))
            .count(stream.batchSize.coerceAtLeast(1).toLong())

        ensureGroupOnce(stream, ops)

        while (currentCoroutineContext().isActive) {
            val records = try {
                withContext(Dispatchers.IO) {
                    ops.read(
                        consumer,
                        options,
                        StreamOffset.create(stream.topic.key, ReadOffset.lastConsumed())
                    )
                } ?: emptyList()
            } catch (e: RedisSystemException) {
                break
            } catch (e: Exception) {
                if (e.message?.contains("Connection closed") == true) break
                log.warn("read failed. streamKey={} group={}", stream.topic, stream.group, e)
                delay(500)
                continue
            }

            for (record in records) {
                handleRecord(stream, ops, record)
            }

            if (records.isEmpty()) delay(50)
        }
    }

    private suspend fun handleRecord(
        stream: StreamDefinition,
        ops: StreamOperations<String, String, String>,
        record: MapRecord<String, String, String>,
    ) {
        val type = record.value["type"]
        val payloadJson = record.value["payload"]

        if (type.isNullOrBlank() || payloadJson.isNullOrBlank()) {
            ack(ops, stream, record.id)
            return
        }

        val handler = handlers.find<Any>(stream, type)
        if (handler == null) {
            log.warn("No handler. streamKey={} group={} type={}", stream.topic, stream.group, type)
            ack(ops, stream, record.id)
            return
        }

        val event = runCatching {
            objectMapper.readValue(payloadJson, handler.payloadClass)
        }.getOrElse { e ->
            log.warn(
                "payload deserialize failed. streamKey={} group={} type={} id={}",
                stream.topic, stream.group, type, record.id, e
            )
            if (stream.ackWhenFail) ack(ops, stream, record.id)
            return
        }

        if (stream.blocking) {
            try {
                handler.handler(event)
                ack(ops, stream, record.id)
            } catch (e: Exception) {
                log.warn(
                    "Handler failed. streamKey={} group={} type={} id={}",
                    stream.topic, stream.group, type, record.id, e
                )
                if (stream.ackWhenFail) ack(ops, stream, record.id)
            }
            return
        }

        scope.launch {
            try {
                handler.handler(event)
                ack(ops, stream, record.id)
            } catch (e: Exception) {
                log.warn(
                    "Handler failed(non-blocking). streamKey={} group={} type={} id={}",
                    stream.topic, stream.group, type, record.id, e
                )
                if (stream.ackWhenFail) ack(ops, stream, record.id)
            }
        }
    }

    private suspend fun ensureGroupOnce(
        stream: StreamDefinition,
        ops: StreamOperations<String, String, String>
    ) = withContext(Dispatchers.IO) {
        if (!ensured.add(EnsureKey(stream.topic.key, stream.group))) return@withContext
        try {
            ops.createGroup(stream.topic.key, ReadOffset.from("0-0"), stream.group)
        } catch (_: Exception) {
            // BUSYGROUP 등은 정상 취급
        }
    }

    private suspend fun ack(
        ops: StreamOperations<String, String, String>,
        stream: StreamDefinition,
        recordId: RecordId
    ) = withContext(Dispatchers.IO) {
        ops.acknowledge(stream.topic.key, stream.group, recordId)
    }
}

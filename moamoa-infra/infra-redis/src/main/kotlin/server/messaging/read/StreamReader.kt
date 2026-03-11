package server.messaging.read

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.*
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.data.redis.connection.stream.*
import org.springframework.data.redis.core.StreamOperations
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import server.messaging.annotation.EventStream
import server.messaging.health.RedisHealthStateManager
import server.messaging.health.RedisRecoveryAction
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Component
internal class StreamReader(
    @param:Qualifier("streamStringRedisTemplate")
    private val redis: StringRedisTemplate,
    private val messageProcessor: StreamMessageProcessor,
    private val healthStateManager: RedisHealthStateManager,
    private val streamGroupEnsurer: StreamGroupEnsurer,
) {
    private val logger = KotlinLogging.logger {}

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val jobs = ConcurrentHashMap<String, Job>()

    private data class ReadContext(
        val stream: EventStream,
        val ops: StreamOperations<String, String, String>,
        val consumer: Consumer,
        val options: StreamReadOptions,
    )

    @PostConstruct
    fun initialize() {
        runBlocking {
            messageProcessor.streams().forEach { stream ->
                streamGroupEnsurer.ensure(stream)
                start(stream)
            }
        }
    }

    @PreDestroy
    fun clear() {
        stopAll()
    }

    fun start(stream: EventStream) {
        val jobKey = "${stream.channel.key}::${stream.consumerGroup}"
        jobs.computeIfAbsent(jobKey) {
            scope.launch { loopForSubscription(stream) }
        }
    }

    fun stopAll() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
        scope.cancel()
    }

    private fun loopForSubscription(stream: EventStream) {
        val context = open(stream)
        var skippedByHealth = false

        while (true) {
            if (healthStateManager.isDegraded()) {
                val result = healthStateManager.tryRecover()

                if (!result) {
                    Thread.sleep(10_000)
                    continue
                }
            }

            val records = try {
                healthStateManager.runSafe {
                    read(context)
                }
            } catch (e: Exception) {
                if (isNoGroupException(e)) {
                    streamGroupEnsurer.ensureForRecovery(context.stream)
                    Thread.sleep(10_000)
                    continue
                }

                logger.warn(e) {
                    "read failed. channelKey=${stream.channel.key} consumerGroup=${stream.consumerGroup}"
                }
                Thread.sleep(500)
                continue
            }

            if (records.isFailure) {
                skippedByHealth = true
                Thread.sleep(10_000)
                continue
            }

            if (skippedByHealth) {
                streamGroupEnsurer.ensureForRecovery(context.stream)
                skippedByHealth = false
            }

            messageProcessor.handleRecords(stream, context.ops, records.getOrThrow(), scope)
        }
    }

    private fun open(stream: EventStream): ReadContext {
        val ops = redis.opsForStream<String, String>()
        val consumerName = "worker-${stream.channel.key}-${stream.consumerGroup}-${UUID.randomUUID()}"
        val consumer = Consumer.from(stream.consumerGroup, consumerName)
        val options = StreamReadOptions.empty()
            .block(Duration.ofSeconds(1))
            .count(stream.batchSize.coerceAtLeast(1).toLong())

        return ReadContext(
            stream = stream,
            ops = ops,
            consumer = consumer,
            options = options,
        )
    }

    private fun read(context: ReadContext): List<MapRecord<String, String, String>> =
        context.ops.read(
            context.consumer,
            context.options,
            StreamOffset.create(context.stream.channel.key, ReadOffset.lastConsumed())
        ) ?: emptyList()

    private fun isNoGroupException(exception: Exception): Boolean =
        exception.message?.contains("NOGROUP", ignoreCase = true) == true

    @Bean
    fun streamReaderRecoveryAction(): RedisRecoveryAction = RedisRecoveryAction {
        messageProcessor.streams().forEach { stream ->
            streamGroupEnsurer.ensureForRecovery(stream)
        }
    }
}

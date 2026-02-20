package server.messaging

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.RedisSystemException
import org.springframework.data.redis.connection.stream.Consumer
import org.springframework.data.redis.connection.stream.MapRecord
import org.springframework.data.redis.connection.stream.ReadOffset
import org.springframework.data.redis.connection.stream.StreamOffset
import org.springframework.data.redis.connection.stream.StreamReadOptions
import org.springframework.data.redis.core.StreamOperations
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import server.shared.messaging.SubscriptionDefinition
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Component
internal class StreamConnection(
    @param:Qualifier("streamStringRedisTemplate")
    private val redis: StringRedisTemplate,
    private val messageProcessor: StreamMessageProcessor,
    private val stateManager: StreamConnectionStateManager,
) {
    private val logger = KotlinLogging.logger {}

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val jobs = ConcurrentHashMap<String, Job>()
    private val ensured = ConcurrentHashMap.newKeySet<EnsureKey>()

    private data class EnsureKey(val channelKey: String, val consumerGroup: String)
    private data class ReadContext(
        val subscription: SubscriptionDefinition,
        val ops: StreamOperations<String, String, String>,
        val consumer: Consumer,
        val options: StreamReadOptions,
        val ensureKey: EnsureKey,
    )

    @PostConstruct
    fun initialize() {
        runBlocking {
            messageProcessor.subscriptions().forEach { subscription ->
                ensureGroup(subscription)
                start(subscription)
            }
        }
    }

    @PreDestroy
    fun clear() {
        stopAll()
    }

    fun start(subscription: SubscriptionDefinition) {
        val jobKey = "${subscription.channel}::${subscription.consumerGroup}"
        jobs.computeIfAbsent(jobKey) {
            scope.launch { loopForSubscription(subscription) }
        }
    }

    suspend fun ensureGroup(subscription: SubscriptionDefinition) {
        val ops = redis.opsForStream<String, String>()
        val ensureKey = EnsureKey(subscription.channel.key, subscription.consumerGroup)
        ensureGroupOnce(subscription, ops, ensureKey)
    }

    fun stopAll() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
        scope.cancel()
    }

    private suspend fun loopForSubscription(subscription: SubscriptionDefinition) {
        val context = open(subscription)
        var state = stateManager.initialState()

        while (currentCoroutineContext().isActive) {
            if (state.mode == StreamConnectionStateManager.ReaderMode.DEGRADED) {
                val now = nowMillis()
                val waitDurationMillis = stateManager.shouldWaitBeforeProbe(state, now)
                if (waitDurationMillis != null) {
                    delay(waitDurationMillis)
                    continue
                }

                val probeResult = probeRedisConnection()
                if (probeResult.isSuccess) {
                    ensureGroupForRecovery(context)
                    state = stateManager.recover()
                    logger.warn {
                        "subscription reader recovered. channelKey=${subscription.channel} consumerGroup=${subscription.consumerGroup}"
                    }
                    continue
                }

                logger.debug(probeResult.exceptionOrNull()) {
                    "subscription reader probe failed. channelKey=${subscription.channel} consumerGroup=${subscription.consumerGroup}"
                }
                state = stateManager.onProbeFailed(nowMillis())
                continue
            }

            val records = try {
                read(context)
            } catch (e: Exception) {
                if (isNoGroupException(e)) {
                    ensureGroupForRecovery(context)
                    delay(200)
                    continue
                }

                if (isRedisFailure(e)) {
                    state = stateManager.enterDegraded(nowMillis())
                    logger.warn(e) {
                        "subscription reader degraded. channelKey=${subscription.channel} consumerGroup=${subscription.consumerGroup} " +
                            "pauseMs=${stateManager.readPauseOnFailureMillis()} probeIntervalMs=${stateManager.recoveryProbeIntervalMillis()}"
                    }
                    continue
                }

                logger.warn(e) {
                    "read failed. channelKey=${subscription.channel} consumerGroup=${subscription.consumerGroup}"
                }
                delay(500)
                continue
            }

            messageProcessor.handleRecords(subscription, context.ops, records, scope)

            if (records.isEmpty()) delay(50)
        }
    }

    private fun nowMillis(): Long = System.currentTimeMillis()

    private suspend fun open(subscription: SubscriptionDefinition): ReadContext {
        val ops = redis.opsForStream<String, String>()
        val consumerName = "worker-${subscription.channel}-${subscription.consumerGroup}-${UUID.randomUUID()}"
        val consumer = Consumer.from(subscription.consumerGroup, consumerName)
        val options = StreamReadOptions.empty()
            .block(Duration.ofSeconds(1))
            .count(subscription.batchSize.coerceAtLeast(1).toLong())
        val ensureKey = EnsureKey(subscription.channel.key, subscription.consumerGroup)

        return ReadContext(
            subscription = subscription,
            ops = ops,
            consumer = consumer,
            options = options,
            ensureKey = ensureKey,
        )
    }

    private suspend fun read(context: ReadContext): List<MapRecord<String, String, String>> =
        withContext(Dispatchers.IO) {
            context.ops.read(
                context.consumer,
                context.options,
                StreamOffset.create(context.subscription.channel.key, ReadOffset.lastConsumed())
            ) ?: emptyList()
        }

    private suspend fun ensureGroupForRecovery(context: ReadContext) {
        ensured.remove(context.ensureKey)
        ensureGroupOnce(context.subscription, context.ops, context.ensureKey)
    }

    private suspend fun probeRedisConnection(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            redis.execute { connection -> connection.ping() }
                ?: throw IllegalStateException("Redis ping returned null")
        }.map { Unit }
    }

    private fun isNoGroupException(exception: Exception): Boolean =
        exception.message?.contains("NOGROUP", ignoreCase = true) == true

    private fun isRedisFailure(exception: Exception): Boolean {
        val message = exception.message.orEmpty()
        return exception is RedisSystemException ||
            message.contains("Connection closed", ignoreCase = true) ||
            message.contains("Unable to connect", ignoreCase = true) ||
            message.contains("connection reset", ignoreCase = true)
    }

    private suspend fun ensureGroupOnce(
        subscription: SubscriptionDefinition,
        ops: StreamOperations<String, String, String>,
        ensureKey: EnsureKey,
    ) = withContext(Dispatchers.IO) {
        if (!ensured.add(ensureKey)) return@withContext
        try {
            ops.createGroup(subscription.channel.key, ReadOffset.from("0-0"), subscription.consumerGroup)
        } catch (_: Exception) {
            // BUSYGROUP 등은 정상 취급
        }
    }
}

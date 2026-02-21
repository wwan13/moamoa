package server.messaging.read

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
import org.springframework.context.annotation.Bean
import org.springframework.data.redis.connection.stream.Consumer
import org.springframework.data.redis.connection.stream.MapRecord
import org.springframework.data.redis.connection.stream.ReadOffset
import org.springframework.data.redis.connection.stream.StreamOffset
import org.springframework.data.redis.connection.stream.StreamReadOptions
import org.springframework.data.redis.core.StreamOperations
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import server.messaging.health.RedisHealthStateManager
import server.messaging.health.RedisRecoveryAction
import server.shared.messaging.SubscriptionDefinition
import java.time.Duration
import java.util.UUID
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
        val subscription: SubscriptionDefinition,
        val ops: StreamOperations<String, String, String>,
        val consumer: Consumer,
        val options: StreamReadOptions,
    )

    @PostConstruct
    fun initialize() {
        runBlocking {
            messageProcessor.subscriptions().forEach { subscription ->
                streamGroupEnsurer.ensure(subscription)
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

    fun stopAll() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
        scope.cancel()
    }

    private suspend fun loopForSubscription(subscription: SubscriptionDefinition) {
        val context = open(subscription)
        var skippedByHealth = false

        while (currentCoroutineContext().isActive) {
            val records = try {
                healthStateManager.runSafe {
                    read(context)
                }
            } catch (e: Exception) {
                if (isNoGroupException(e)) {
                    streamGroupEnsurer.ensureForRecovery(context.subscription)
                    delay(200)
                    continue
                }

                logger.warn(e) {
                    "read failed. channelKey=${subscription.channel} consumerGroup=${subscription.consumerGroup}"
                }
                delay(500)
                continue
            }

            if (records.isFailure) {
                skippedByHealth = true
                delay(200)
                continue
            }
            val safeRecords = records.getOrThrow()

            if (skippedByHealth) {
                streamGroupEnsurer.ensureForRecovery(context.subscription)
                skippedByHealth = false
            }

            messageProcessor.handleRecords(subscription, context.ops, safeRecords, scope)

            if (safeRecords.isEmpty()) delay(50)
        }
    }

    private suspend fun open(subscription: SubscriptionDefinition): ReadContext {
        val ops = redis.opsForStream<String, String>()
        val consumerName = "worker-${subscription.channel}-${subscription.consumerGroup}-${UUID.randomUUID()}"
        val consumer = Consumer.from(subscription.consumerGroup, consumerName)
        val options = StreamReadOptions.empty()
            .block(Duration.ofSeconds(1))
            .count(subscription.batchSize.coerceAtLeast(1).toLong())

        return ReadContext(
            subscription = subscription,
            ops = ops,
            consumer = consumer,
            options = options,
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

    private fun isNoGroupException(exception: Exception): Boolean =
        exception.message?.contains("NOGROUP", ignoreCase = true) == true

    @Bean
    fun streamReaderRecoveryAction(): RedisRecoveryAction = RedisRecoveryAction {
        messageProcessor.subscriptions().forEach { subscription ->
            streamGroupEnsurer.ensureForRecovery(subscription)
        }
    }
}

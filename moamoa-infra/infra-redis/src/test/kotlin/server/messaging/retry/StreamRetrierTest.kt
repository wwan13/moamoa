package server.messaging.retry

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Range
import org.springframework.data.redis.connection.stream.PendingMessage
import org.springframework.data.redis.RedisConnectionFailureException
import org.springframework.data.redis.connection.stream.PendingMessages
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.ReactiveStreamOperations
import org.springframework.data.redis.core.RedisCallback
import org.springframework.data.redis.core.StringRedisTemplate
import reactor.core.publisher.Mono
import server.config.StreamRetryProperties
import server.messaging.health.RedisRecoveryActionRunner
import server.messaging.health.RedisHealthStateManager
import server.messaging.StreamEventHandlers
import server.shared.messaging.MessageChannel
import server.shared.messaging.SubscriptionDefinition

class StreamRetrierTest {

    @Test
    fun `DEGRADED 상태에서는 pending 조회를 시도하지 않는다`() = runBlocking {
        val redis = mockk<StringRedisTemplate>()
        every { redis.execute(any<RedisCallback<String>>()) } throws RedisConnectionFailureException("redis down")
        val healthStateManager = newHealthManager(redis)
        healthStateManager.runSafe {
            throw RedisConnectionFailureException("redis down")
        }

        val eventHandlers = mockk<StreamEventHandlers>()
        val subscription = SubscriptionDefinition(
            channel = MessageChannel("retry-test"),
            consumerGroup = "retry-group",
        )
        every { eventHandlers.subscriptions() } returns listOf(subscription)

        val streamOps = mockk<ReactiveStreamOperations<String, String, String>>()
        val redisTemplate = mockk<ReactiveRedisTemplate<String, String>>()
        every { redisTemplate.opsForStream<String, String>() } returns streamOps
        val retryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        val retrier = StreamRetrier(
            schedulerScope = retryScope,
            retryProcessor = StreamRetryProcessor(
                redisTemplate = redisTemplate,
                defaultDlqTopic = MessageChannel("moamoa-default-dlq"),
                objectMapper = jacksonObjectMapper(),
                eventHandlers = eventHandlers,
                healthStateManager = healthStateManager,
            ),
            properties = StreamRetryProperties(intervalMs = 60_000),
        )

        try {
            retrier.runOnce()

            verify(exactly = 0) {
                streamOps.pending(any<String>(), any<String>(), any<Range<String>>(), any<Long>())
            }
        } finally {
            retryScope.cancel()
        }
    }

    @Test
    fun `복구 후 runOnce 호출 시 pending 조회를 재개한다`() = runBlocking {
        val redis = mockk<StringRedisTemplate>()
        every { redis.execute(any<RedisCallback<String>>()) } returns "PONG"
        val healthStateManager = newHealthManager(redis)
        healthStateManager.runSafe {
            throw RedisConnectionFailureException("redis down")
        }
        healthStateManager.tryRecover()

        val eventHandlers = mockk<StreamEventHandlers>()
        val subscription = SubscriptionDefinition(
            channel = MessageChannel("retry-test"),
            consumerGroup = "retry-group",
        )
        every { eventHandlers.subscriptions() } returns listOf(subscription)

        val pending = mockk<PendingMessages>()
        every { pending.toList() } returns emptyList<PendingMessage>()

        val streamOps = mockk<ReactiveStreamOperations<String, String, String>>()
        every {
            streamOps.pending(any<String>(), any<String>(), any<Range<String>>(), any<Long>())
        } returns Mono.just(pending)

        val redisTemplate = mockk<ReactiveRedisTemplate<String, String>>()
        every { redisTemplate.opsForStream<String, String>() } returns streamOps
        val retryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        val retrier = StreamRetrier(
            schedulerScope = retryScope,
            retryProcessor = StreamRetryProcessor(
                redisTemplate = redisTemplate,
                defaultDlqTopic = MessageChannel("moamoa-default-dlq"),
                objectMapper = jacksonObjectMapper(),
                eventHandlers = eventHandlers,
                healthStateManager = healthStateManager,
            ),
            properties = StreamRetryProperties(intervalMs = 60_000),
        )

        try {
            retrier.runOnce()

            verify(exactly = 1) {
                streamOps.pending(any<String>(), any<String>(), any<Range<String>>(), any<Long>())
            }
        } finally {
            retryScope.cancel()
        }
    }

    private fun newHealthManager(redis: StringRedisTemplate): RedisHealthStateManager {
        return RedisHealthStateManager(
            redis = redis,
            recoveryActionRunner = mockk<RedisRecoveryActionRunner>(relaxed = true).also { runner ->
                coEvery { runner.runAll() } returns Unit
            },
        )
    }
}

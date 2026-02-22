package server.messaging.retry

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Range
import org.springframework.data.redis.RedisConnectionFailureException
import org.springframework.data.redis.connection.stream.MapRecord
import org.springframework.data.redis.connection.stream.PendingMessage
import org.springframework.data.redis.connection.stream.PendingMessages
import org.springframework.data.redis.connection.stream.RecordId
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.ReactiveStreamOperations
import org.springframework.data.redis.core.RedisCallback
import org.springframework.data.redis.core.StringRedisTemplate
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import server.messaging.StreamEventHandlers
import server.messaging.health.RedisRecoveryActionRunner
import server.messaging.health.RedisHealthStateManager
import server.shared.messaging.MessageChannel
import server.shared.messaging.MessageHandlerBinding
import server.shared.messaging.SubscriptionDefinition
import java.time.Duration

class StreamRetryProcessorTest {

    @Test
    fun `deliveryCount가 3 이하면 기존 재처리를 수행한다`() = runBlocking {
        val subscription = SubscriptionDefinition(
            channel = MessageChannel("retry-test"),
            consumerGroup = "retry-group",
        )
        val eventHandlers = mockk<StreamEventHandlers>()
        every { eventHandlers.subscriptions() } returns listOf(subscription)

        var handled = false
        val handlerBinding = MessageHandlerBinding(
            subscription = subscription,
            type = "TestPayload",
            payloadClass = TestPayload::class.java,
            handler = { handled = true },
        )
        every { eventHandlers.find<Any>(subscription, "TestPayload") } returns handlerBinding as MessageHandlerBinding<Any>

        val pendingMessage = mockk<PendingMessage>()
        every { pendingMessage.id } returns RecordId.of("1-0")
        every { pendingMessage.totalDeliveryCount } returns 3L
        every { pendingMessage.elapsedTimeSinceLastDelivery } returns Duration.ofMinutes(6)

        val pending = mockk<PendingMessages>()
        every { pending.toList() } returns listOf(pendingMessage)

        val claimedRecord = mockk<MapRecord<String, String, String>>()
        every { claimedRecord.id } returns RecordId.of("1-0")
        every { claimedRecord.value } returns mapOf(
            "type" to "TestPayload",
            "payload" to """{"value":"ok"}""",
        )

        val streamOps = mockk<ReactiveStreamOperations<String, String, String>>()
        every {
            streamOps.pending(any<String>(), any<String>(), any<Range<String>>(), any<Long>())
        } returns Mono.just(pending)
        every {
            streamOps.claim(any<String>(), any<String>(), any<String>(), any<Duration>(), any<RecordId>())
        } returns Flux.just(claimedRecord)
        every { streamOps.acknowledge(any<String>(), any<String>(), any<RecordId>()) } returns Mono.just(1L)

        val redisTemplate = mockk<ReactiveRedisTemplate<String, String>>()
        every { redisTemplate.opsForStream<String, String>() } returns streamOps

        val processor = StreamRetryProcessor(
            redisTemplate = redisTemplate,
            defaultDlqTopic = MessageChannel("moamoa-default-dlq"),
            objectMapper = jacksonObjectMapper(),
            eventHandlers = eventHandlers,
            healthStateManager = newHealthManager(),
        )

        val result = processor.processOnce()

        assertTrue(result)
        assertTrue(handled)
        verify(exactly = 0) { streamOps.add(any<MapRecord<String, String, String>>()) }
        verify(exactly = 1) { streamOps.acknowledge(any<String>(), any<String>(), any<RecordId>()) }
    }

    @Test
    fun `deliveryCount가 3회를 초과하면 DLQ로 이동 후 ACK 한다`() = runBlocking {
        val subscription = SubscriptionDefinition(
            channel = MessageChannel("retry-test"),
            consumerGroup = "retry-group",
        )
        val eventHandlers = mockk<StreamEventHandlers>()
        every { eventHandlers.subscriptions() } returns listOf(subscription)

        val pendingMessage = mockk<PendingMessage>()
        every { pendingMessage.id } returns RecordId.of("2-0")
        every { pendingMessage.totalDeliveryCount } returns 4L
        every { pendingMessage.elapsedTimeSinceLastDelivery } returns Duration.ofMinutes(6)

        val pending = mockk<PendingMessages>()
        every { pending.toList() } returns listOf(pendingMessage)

        val claimedRecord = mockk<MapRecord<String, String, String>>()
        every { claimedRecord.id } returns RecordId.of("2-0")
        every { claimedRecord.value } returns mapOf(
            "type" to "TestPayload",
            "payload" to """{"value":"dlq"}""",
        )

        val streamOps = mockk<ReactiveStreamOperations<String, String, String>>()
        every {
            streamOps.pending(any<String>(), any<String>(), any<Range<String>>(), any<Long>())
        } returns Mono.just(pending)
        every {
            streamOps.claim(any<String>(), any<String>(), any<String>(), any<Duration>(), any<RecordId>())
        } returns Flux.just(claimedRecord)
        every { streamOps.add(any<MapRecord<String, String, String>>()) } returns Mono.just(RecordId.of("10-0"))
        every { streamOps.acknowledge(any<String>(), any<String>(), any<RecordId>()) } returns Mono.just(1L)

        val redisTemplate = mockk<ReactiveRedisTemplate<String, String>>()
        every { redisTemplate.opsForStream<String, String>() } returns streamOps

        val processor = StreamRetryProcessor(
            redisTemplate = redisTemplate,
            defaultDlqTopic = MessageChannel("moamoa-default-dlq"),
            objectMapper = jacksonObjectMapper(),
            eventHandlers = eventHandlers,
            healthStateManager = newHealthManager(),
        )

        val result = processor.processOnce()

        assertTrue(result)
        verify(exactly = 1) {
            streamOps.add(
                match<MapRecord<String, String, String>> {
                    it.stream == "moamoa-default-dlq" &&
                        it.value["type"] == "TestPayload" &&
                        it.value["payload"] == """{"value":"dlq"}""" &&
                        it.value["sourceChannel"] == "retry-test" &&
                        it.value["sourceGroup"] == "retry-group" &&
                        it.value["sourceId"] == "2-0" &&
                        it.value["deliveryCount"] == "4"
                }
            )
        }
        verify(exactly = 1) { streamOps.acknowledge(any<String>(), any<String>(), any<RecordId>()) }
        verify(exactly = 0) { eventHandlers.find<Any>(subscription, any()) }
    }

    @Test
    fun `DLQ append 실패 시 ACK 하지 않고 false 를 반환한다`() = runBlocking {
        val subscription = SubscriptionDefinition(
            channel = MessageChannel("retry-test"),
            consumerGroup = "retry-group",
        )
        val eventHandlers = mockk<StreamEventHandlers>()
        every { eventHandlers.subscriptions() } returns listOf(subscription)

        val pendingMessage = mockk<PendingMessage>()
        every { pendingMessage.id } returns RecordId.of("3-0")
        every { pendingMessage.totalDeliveryCount } returns 4L
        every { pendingMessage.elapsedTimeSinceLastDelivery } returns Duration.ofMinutes(6)

        val pending = mockk<PendingMessages>()
        every { pending.toList() } returns listOf(pendingMessage)

        val claimedRecord = mockk<MapRecord<String, String, String>>()
        every { claimedRecord.id } returns RecordId.of("3-0")
        every { claimedRecord.value } returns mapOf(
            "type" to "TestPayload",
            "payload" to """{"value":"dlq"}""",
        )

        val streamOps = mockk<ReactiveStreamOperations<String, String, String>>()
        every {
            streamOps.pending(any<String>(), any<String>(), any<Range<String>>(), any<Long>())
        } returns Mono.just(pending)
        every {
            streamOps.claim(any<String>(), any<String>(), any<String>(), any<Duration>(), any<RecordId>())
        } returns Flux.just(claimedRecord)
        every { streamOps.add(any<MapRecord<String, String, String>>()) } returns
            Mono.error(RedisConnectionFailureException("dlq down"))

        val redisTemplate = mockk<ReactiveRedisTemplate<String, String>>()
        every { redisTemplate.opsForStream<String, String>() } returns streamOps

        val processor = StreamRetryProcessor(
            redisTemplate = redisTemplate,
            defaultDlqTopic = MessageChannel("moamoa-default-dlq"),
            objectMapper = jacksonObjectMapper(),
            eventHandlers = eventHandlers,
            healthStateManager = newHealthManager(),
        )

        val result = processor.processOnce()

        assertFalse(result)
        verify(exactly = 1) { streamOps.add(any<MapRecord<String, String, String>>()) }
        verify(exactly = 0) { streamOps.acknowledge(any<String>(), any<String>(), any<RecordId>()) }
    }

    private fun newHealthManager(): RedisHealthStateManager {
        val redis = mockk<StringRedisTemplate>()
        every { redis.execute(any<RedisCallback<String>>()) } returns "PONG"

        return RedisHealthStateManager(
            redis = redis,
            recoveryActionRunner = mockk<RedisRecoveryActionRunner>(relaxed = true).also { runner ->
                coEvery { runner.runAll() } returns Unit
            },
        )
    }

    private data class TestPayload(
        val value: String,
    )
}

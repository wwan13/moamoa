package server.messaging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.data.redis.connection.stream.Consumer
import org.springframework.data.redis.connection.stream.StreamOffset
import org.springframework.data.redis.connection.stream.StreamReadOptions
import org.springframework.data.redis.core.RedisCallback
import org.springframework.data.redis.core.StreamOperations
import org.springframework.data.redis.core.StringRedisTemplate
import server.config.SubscriptionReaderProperties
import server.shared.messaging.MessageChannel
import server.shared.messaging.MessageHandlerBinding
import server.shared.messaging.SubscriptionDefinition
import kotlinx.coroutines.runBlocking

class StreamConnectionTest {
    private val subscription = SubscriptionDefinition(
        channel = MessageChannel("subscription-reader-test"),
        consumerGroup = "subscription-reader-group",
    )

    @Test
    fun `Redis read 실패 시 cooldown 동안 read 재시도를 멈춘다`() {
        val redis = mockk<StringRedisTemplate>()
        val ops = mockk<StreamOperations<String, String, String>>()
        every { redis.opsForStream<String, String>() } returns ops
        every { ops.createGroup(any(), any(), any()) } returns "OK"
        every {
            ops.read(any<Consumer>(), any<StreamReadOptions>(), any<StreamOffset<String>>())
        } throws IllegalStateException("Connection closed")

        val reader = newConnection(redis, readPauseOnFailureMs = 200, recoveryProbeIntervalMs = 50)
        try {
            reader.start(subscription)
            Thread.sleep(120)
        } finally {
            reader.stopAll()
        }

        verify(exactly = 1) { ops.read(any<Consumer>(), any<StreamReadOptions>(), any<StreamOffset<String>>()) }
    }

    @Test
    fun `cooldown 이후 probe 실패 시 probe 간격 전에는 재시도하지 않는다`() {
        val redis = mockk<StringRedisTemplate>()
        val ops = mockk<StreamOperations<String, String, String>>()
        every { redis.opsForStream<String, String>() } returns ops
        every { ops.createGroup(any(), any(), any()) } returns "OK"
        every {
            ops.read(any<Consumer>(), any<StreamReadOptions>(), any<StreamOffset<String>>())
        } throws IllegalStateException("Connection closed")
        every { redis.execute(any<RedisCallback<String>>()) } throws IllegalStateException("redis down")

        val reader = newConnection(redis, readPauseOnFailureMs = 30, recoveryProbeIntervalMs = 120)
        try {
            reader.start(subscription)
            Thread.sleep(100)
        } finally {
            reader.stopAll()
        }

        verify(exactly = 1) { redis.execute(any<RedisCallback<String>>()) }
    }

    @Test
    fun `probe 성공 시 ACTIVE로 복귀하고 read를 재개하며 group ensure를 다시 수행한다`() {
        val redis = mockk<StringRedisTemplate>()
        val ops = mockk<StreamOperations<String, String, String>>()
        every { redis.opsForStream<String, String>() } returns ops
        every { ops.createGroup(any(), any(), any()) } returns "OK"
        every {
            ops.read(any<Consumer>(), any<StreamReadOptions>(), any<StreamOffset<String>>())
        } throws IllegalStateException("Connection closed") andThen emptyList()
        every { redis.execute(any<RedisCallback<String>>()) } returns "PONG"

        val reader = newConnection(redis, readPauseOnFailureMs = 30, recoveryProbeIntervalMs = 20)
        try {
            reader.start(subscription)
            Thread.sleep(140)
        } finally {
            reader.stopAll()
        }

        verify(atLeast = 2) { ops.read(any<Consumer>(), any<StreamReadOptions>(), any<StreamOffset<String>>()) }
        verify(exactly = 2) { ops.createGroup(any(), any(), any()) }
    }

    @Test
    fun `장애 상태에서는 최초 WARN 이후 probe 실패를 WARN으로 남기지 않는다`() {
        val redis = mockk<StringRedisTemplate>()
        val ops = mockk<StreamOperations<String, String, String>>()
        every { redis.opsForStream<String, String>() } returns ops
        every { ops.createGroup(any(), any(), any()) } returns "OK"
        every {
            ops.read(any<Consumer>(), any<StreamReadOptions>(), any<StreamOffset<String>>())
        } throws IllegalStateException("Connection closed")
        every { redis.execute(any<RedisCallback<String>>()) } throws IllegalStateException("redis down")

        val logger = LoggerFactory.getLogger(StreamConnection::class.java) as Logger
        val appender = ListAppender<ILoggingEvent>()
        appender.start()
        logger.addAppender(appender)

        val reader = newConnection(redis, readPauseOnFailureMs = 30, recoveryProbeIntervalMs = 20)
        try {
            reader.start(subscription)
            Thread.sleep(120)
        } finally {
            reader.stopAll()
            logger.detachAppender(appender)
        }

        val degradedWarnLogs = appender.list.filter { it.level == Level.WARN && it.formattedMessage.contains("degraded") }
        val probeWarnLogs = appender.list.filter { it.level == Level.WARN && it.formattedMessage.contains("probe failed") }

        degradedWarnLogs.size shouldBe 1
        probeWarnLogs.size shouldBe 0
    }

    private fun newConnection(
        redis: StringRedisTemplate,
        readPauseOnFailureMs: Long,
        recoveryProbeIntervalMs: Long,
    ): StreamConnection {
        val binding = MessageHandlerBinding(
            subscription = subscription,
            type = "dummy",
            payloadClass = String::class.java,
            handler = {}
        )
        val handlers = StreamEventHandlers(listOf(binding))
        val messageProcessor = StreamMessageProcessor(handlers, jacksonObjectMapper())
        val properties = SubscriptionReaderProperties(
            readPauseOnFailureMs = readPauseOnFailureMs,
            recoveryProbeIntervalMs = recoveryProbeIntervalMs,
        )
        return StreamConnection(
            redis = redis,
            messageProcessor = messageProcessor,
            stateManager = StreamConnectionStateManager(properties),
        ).also { connection ->
            runBlocking { connection.ensureGroup(subscription) }
        }
    }
}

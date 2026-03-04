package server.messaging.retry

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.StreamOperations
import org.springframework.data.redis.core.StringRedisTemplate
import server.messaging.StreamEventHandlers
import server.messaging.health.RedisHealthStateManager
import server.shared.messaging.MessageChannel

class StreamRetryProcessorTest {

    @Test
    fun `DEGRADED 상태에서 복구 실패 시 false를 반환한다`() {
        val processor = newProcessor(
            healthStateManager = mockk<RedisHealthStateManager>().also {
                every { it.isDegraded() } returns true
                every { it.tryRecover() } returns false
            },
            eventHandlers = mockk<StreamEventHandlers>().also {
                every { it.subscriptions() } returns emptyList()
            }
        )

        val result = processor.processOnce()

        assertFalse(result)
    }

    @Test
    fun `구독이 없으면 true를 반환한다`() {
        val health = mockk<RedisHealthStateManager>()
        every { health.isDegraded() } returns false

        val processor = newProcessor(
            healthStateManager = health,
            eventHandlers = mockk<StreamEventHandlers>().also {
                every { it.subscriptions() } returns emptyList()
            }
        )

        val result = processor.processOnce()

        assertTrue(result)
        verify(exactly = 1) { health.isDegraded() }
    }

    private fun newProcessor(
        healthStateManager: RedisHealthStateManager,
        eventHandlers: StreamEventHandlers,
    ): StreamRetryProcessor {
        val streamOps = mockk<StreamOperations<String, String, String>>(relaxed = true)
        val redisTemplate = mockk<StringRedisTemplate>()
        every { redisTemplate.opsForStream<String, String>() } returns streamOps

        return StreamRetryProcessor(
            redisTemplate = redisTemplate,
            defaultDlqTopic = MessageChannel("moamoa-default-dlq"),
            objectMapper = jacksonObjectMapper(),
            eventHandlers = eventHandlers,
            healthStateManager = healthStateManager,
        )
    }
}

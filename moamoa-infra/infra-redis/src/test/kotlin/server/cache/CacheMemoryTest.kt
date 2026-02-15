package server.cache

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.ReactiveValueOperations
import org.springframework.data.redis.core.script.RedisScript
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration

class CacheMemoryTest {
    private val redisTemplate = mockk<ReactiveRedisTemplate<String, String>>()
    private val valueOps = mockk<ReactiveValueOperations<String, String>>()
    private val objectMapper = jacksonObjectMapper()
    private val cacheMemory = CacheMemory(redisTemplate, objectMapper)

    @Test
    fun `mget은 키 목록이 비어있으면 빈 맵을 반환한다`() = runTest {
        val result = cacheMemory.mget(emptyList())

        result shouldBe emptyMap()
        verify(exactly = 0) { redisTemplate.opsForValue() }
        verify(exactly = 0) { redisTemplate.execute(any<RedisScript<List<*>>>(), any<List<String>>(), any<List<String>>()) }
    }

    @Test
    fun `mget은 Lua 실행 성공 시 키 순서대로 값을 매핑한다`() = runTest {
        val keys = listOf("k1", "k2")

        every { redisTemplate.execute(any<RedisScript<List<*>>>(), keys, emptyList<String>()) } returns
            Flux.just(listOf("v1", null))

        val result = cacheMemory.mget(keys)

        result shouldBe mapOf("k1" to "v1", "k2" to null)
        verify(exactly = 1) { redisTemplate.execute(any<RedisScript<List<*>>>(), keys, emptyList<String>()) }
        verify(exactly = 0) { redisTemplate.opsForValue() }
    }

    @Test
    fun `mget은 Lua 실패 시 multiGet으로 폴백한다`() = runTest {
        val keys = listOf("k1", "k2")

        every { redisTemplate.opsForValue() } returns valueOps
        every { redisTemplate.execute(any<RedisScript<List<*>>>(), keys, emptyList<String>()) } throws RuntimeException("lua error")
        every { valueOps.multiGet(keys) } returns Mono.just(listOf("v1", null))

        val result = cacheMemory.mget(keys)

        result shouldBe mapOf("k1" to "v1", "k2" to null)
        verify(exactly = 1) { redisTemplate.execute(any<RedisScript<List<*>>>(), keys, emptyList<String>()) }
        verify(exactly = 1) { valueOps.multiGet(keys) }
    }

    @Test
    fun `mset은 데이터가 비어있으면 저장을 건너뛴다`() = runTest {
        cacheMemory.mset(emptyMap())

        verify(exactly = 0) { redisTemplate.execute(any<RedisScript<Long>>(), any<List<String>>(), any<List<String>>()) }
        verify(exactly = 0) { redisTemplate.opsForValue() }
    }

    @Test
    fun `mset은 Lua 실행 성공 시 스크립트로 일괄 저장한다`() = runTest {
        val payload = linkedMapOf("k1" to 10, "k2" to 20)
        val keys = listOf("k1", "k2")
        val args = listOf("60000", "10", "20")

        every { redisTemplate.execute(any<RedisScript<Long>>(), keys, args) } returns Flux.just(2L)

        cacheMemory.mset(payload, 60_000L)

        verify(exactly = 1) { redisTemplate.execute(any<RedisScript<Long>>(), keys, args) }
        verify(exactly = 0) { redisTemplate.opsForValue() }
    }

    @Test
    fun `mset은 Lua 실패 시 TTL 없이 multiSet으로 폴백한다`() = runTest {
        val payload = linkedMapOf("k1" to 10, "k2" to 20)
        val keys = listOf("k1", "k2")
        val args = listOf("-1", "10", "20")

        every { redisTemplate.opsForValue() } returns valueOps
        every { redisTemplate.execute(any<RedisScript<Long>>(), keys, args) } throws RuntimeException("lua error")
        every { valueOps.multiSet(mapOf("k1" to "10", "k2" to "20")) } returns Mono.just(true)

        cacheMemory.mset(payload, null)

        verify(exactly = 1) { redisTemplate.execute(any<RedisScript<Long>>(), keys, args) }
        verify(exactly = 1) { valueOps.multiSet(mapOf("k1" to "10", "k2" to "20")) }
        verify(exactly = 0) { redisTemplate.expire(any<String>(), any<Duration>()) }
    }

    @Test
    fun `mset은 Lua 실패 시 TTL과 함께 multiSet 후 expire로 폴백한다`() = runTest {
        val payload = linkedMapOf("k1" to 10, "k2" to 20)
        val keys = listOf("k1", "k2")
        val args = listOf("60000", "10", "20")
        val duration = Duration.ofMillis(60_000L)

        every { redisTemplate.opsForValue() } returns valueOps
        every { redisTemplate.execute(any<RedisScript<Long>>(), keys, args) } throws RuntimeException("lua error")
        every { valueOps.multiSet(mapOf("k1" to "10", "k2" to "20")) } returns Mono.just(true)
        every { redisTemplate.expire("k1", duration) } returns Mono.just(true)
        every { redisTemplate.expire("k2", duration) } returns Mono.just(true)

        cacheMemory.mset(payload, 60_000L)

        verify(exactly = 1) { redisTemplate.execute(any<RedisScript<Long>>(), keys, args) }
        verify(exactly = 1) { valueOps.multiSet(mapOf("k1" to "10", "k2" to "20")) }
        verify(exactly = 1) { redisTemplate.expire("k1", duration) }
        verify(exactly = 1) { redisTemplate.expire("k2", duration) }
    }

    @Test
    fun `decrBy는 increment 음수 연산을 사용한다`() = runTest {
        every { redisTemplate.opsForValue() } returns valueOps
        every { valueOps.increment("k1", -3L) } returns Mono.just(7L)

        val result = cacheMemory.decrBy("k1", 3L)

        result shouldBe 7L
        verify(exactly = 1) { valueOps.increment("k1", -3L) }
    }
}

package server.cache

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations

class CacheMemoryTest {
    private val redis = mockk<StringRedisTemplate>()
    private val valueOps = mockk<ValueOperations<String, String>>()
    private val objectMapper = jacksonObjectMapper()
    private val cacheMemory = RedisCacheMemory(redis, objectMapper)

    @Test
    fun `mget은 키 목록이 비어있으면 빈 맵을 반환한다`() {
        val result = cacheMemory.mget(emptyList())

        result shouldBe emptyMap()
        verify(exactly = 0) { redis.opsForValue() }
    }

    @Test
    fun `decrBy는 increment 음수 연산을 사용한다`() {
        every { redis.opsForValue() } returns valueOps
        every { valueOps.increment("k1", -3L) } returns 7L

        val result = cacheMemory.decrBy("k1", 3L)

        result shouldBe 7L
        verify(exactly = 1) { valueOps.increment("k1", -3L) }
    }

    @Test
    fun `Redis 호출 예외는 CacheInfraException으로 래핑한다`() {
        every { redis.opsForValue() } returns valueOps
        every { valueOps.set("k1", "\"v1\"") } throws IllegalStateException("redis down")

        val ex = shouldThrow<CacheInfraException> {
            cacheMemory.set("k1", "v1", null)
        }

        ex.message shouldBe "Cache write failed. key=k1"
        (ex.cause is IllegalStateException) shouldBe true
    }
}

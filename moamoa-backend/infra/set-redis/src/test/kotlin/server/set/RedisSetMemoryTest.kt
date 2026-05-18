package server.set

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.SetOperations
import org.springframework.data.redis.core.StringRedisTemplate
import test.UnitTest

class RedisSetMemoryTest : UnitTest() {
    @Test
    fun `contains 는 set membership 을 반환한다`() {
        val redis = mockk<StringRedisTemplate>()
        val ops = mockk<SetOperations<String, String>>()
        every { redis.opsForSet() } returns ops
        every { ops.isMember("MEMBER:BLACKLIST", "10") } returns true
        val memory = RedisSetMemory(redis)

        val result = memory.contains("MEMBER:BLACKLIST", "10")

        result shouldBe true
    }

    @Test
    fun `contains 는 null 응답이면 false 를 반환한다`() {
        val redis = mockk<StringRedisTemplate>()
        val ops = mockk<SetOperations<String, String>>()
        every { redis.opsForSet() } returns ops
        every { ops.isMember("MEMBER:BLACKLIST", "10") } returns null
        val memory = RedisSetMemory(redis)

        val result = memory.contains("MEMBER:BLACKLIST", "10")

        result shouldBe false
    }
}

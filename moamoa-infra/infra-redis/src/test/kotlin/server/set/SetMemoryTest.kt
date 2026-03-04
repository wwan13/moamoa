package server.set

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.SetOperations
import org.springframework.data.redis.core.StringRedisTemplate

class SetMemoryTest {

    @Test
    fun `add는 추가 성공 여부를 반환한다`() {
        val redis = mockk<StringRedisTemplate>()
        val ops = mockk<SetOperations<String, String>>()
        every { redis.opsForSet() } returns ops
        every { ops.add("dirty", "10") } returns 1L

        val sut = RedisSetMemory(redis)
        val result = sut.add("dirty", "10")

        result shouldBe true
    }

    @Test
    fun `members는 set 멤버를 반환한다`() {
        val redis = mockk<StringRedisTemplate>()
        val ops = mockk<SetOperations<String, String>>()
        every { redis.opsForSet() } returns ops
        every { ops.members("dirty") } returns setOf("10", "20")

        val sut = RedisSetMemory(redis)
        val result = sut.members("dirty")

        result shouldBe setOf("10", "20")
    }

    @Test
    fun `remove는 제거된 개수를 반환한다`() {
        val redis = mockk<StringRedisTemplate>()
        val ops = mockk<SetOperations<String, String>>()
        every { redis.opsForSet() } returns ops
        every { ops.remove("dirty", "10") } returns 1L

        val sut = RedisSetMemory(redis)
        val result = sut.remove("dirty", "10")

        result shouldBe 1L
    }
}

package server.set

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.ReactiveSetOperations
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class SetMemoryTest {

    @Test
    fun `add는 추가 성공 여부를 반환한다`() = runTest {
        val redis = mockk<ReactiveRedisTemplate<String, String>>()
        val ops = mockk<ReactiveSetOperations<String, String>>()
        every { redis.opsForSet() } returns ops
        val sut = RedisSetMemory(redis)
        every { ops.add("dirty", "10") } returns Mono.just(1L)

        val result = sut.add("dirty", "10")

        result shouldBe true
    }

    @Test
    fun `members는 set 멤버를 반환한다`() = runTest {
        val redis = mockk<ReactiveRedisTemplate<String, String>>()
        val ops = mockk<ReactiveSetOperations<String, String>>()
        every { redis.opsForSet() } returns ops
        val sut = RedisSetMemory(redis)
        every { ops.members("dirty") } returns Flux.just("10", "20")

        val result = sut.members("dirty")

        result shouldBe setOf("10", "20")
    }

    @Test
    fun `remove는 제거된 개수를 반환한다`() = runTest {
        val redis = mockk<ReactiveRedisTemplate<String, String>>()
        val ops = mockk<ReactiveSetOperations<String, String>>()
        every { redis.opsForSet() } returns ops
        val sut = RedisSetMemory(redis)
        every { ops.remove("dirty", "10") } returns Mono.just(1L)

        val result = sut.remove("dirty", "10")

        result shouldBe 1L
    }
}

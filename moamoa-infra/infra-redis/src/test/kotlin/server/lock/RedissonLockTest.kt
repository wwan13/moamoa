package server.lock

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.redisson.api.RFuture
import org.redisson.api.RLock
import org.redisson.api.RedissonClient
import server.shared.lock.LockInfraException
import test.UnitTest
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class RedissonLockTest : UnitTest() {
    @Test
    fun `락 획득 후 블록 실행이 완료되면 락을 해제한다`() = runTest {
        val client = mockk<RedissonClient>()
        val lock = mockk<RLock>()
        val redissonLock = RedissonLock(client)

        every { client.getLock("moamoa:lock:k1") } returns lock
        every { lock.tryLockAsync(3_000L, 10_000L, TimeUnit.MILLISECONDS, any()) } returns completedFuture(true)
        every { lock.unlockAsync(any()) } returns completedFuture(null)

        val result = redissonLock.withLock("k1") { "ok" }

        result shouldBe "ok"
        verify(exactly = 1) { lock.tryLockAsync(3_000L, 10_000L, TimeUnit.MILLISECONDS, any()) }
        verify(exactly = 1) { lock.unlockAsync(any()) }
    }

    @Test
    fun `락 획득 중 인프라 예외 발생 시 LockInfraException으로 변환한다`() = runTest {
        val client = mockk<RedissonClient>()
        val lock = mockk<RLock>()
        val redissonLock = RedissonLock(client)

        every { client.getLock("moamoa:lock:k1") } returns lock
        every { lock.tryLockAsync(3_000L, 10_000L, TimeUnit.MILLISECONDS, any()) } returns failedFuture(IllegalStateException("down"))

        val exception = shouldThrow<LockInfraException> {
            redissonLock.withLock("k1") { "nope" }
        }

        exception.message shouldBe "Distributed lock acquire failed. key=moamoa:lock:k1"
    }

    @Test
    fun `락 획득 결과가 false면 IllegalStateException을 던진다`() = runTest {
        val client = mockk<RedissonClient>()
        val lock = mockk<RLock>()
        val redissonLock = RedissonLock(client)

        every { client.getLock("moamoa:lock:k1") } returns lock
        every { lock.tryLockAsync(3_000L, 10_000L, TimeUnit.MILLISECONDS, any()) } returns completedFuture(false)

        val exception = shouldThrow<IllegalStateException> {
            redissonLock.withLock("k1") { "ok" }
        }

        exception.message shouldBe "Distributed lock not acquired. key=moamoa:lock:k1"
        verify(exactly = 0) { lock.unlockAsync(any()) }
    }

    @Test
    fun `락 해제 실패 시 LockInfraException으로 변환한다`() = runTest {
        val client = mockk<RedissonClient>()
        val lock = mockk<RLock>()
        val redissonLock = RedissonLock(client)

        every { client.getLock("moamoa:lock:k1") } returns lock
        every { lock.tryLockAsync(3_000L, 10_000L, TimeUnit.MILLISECONDS, any()) } returns completedFuture(true)
        every { lock.unlockAsync(any()) } returns failedFuture(IllegalStateException("release-fail"))

        val exception = shouldThrow<LockInfraException> {
            redissonLock.withLock("k1") { "ok" }
        }

        exception.message shouldBe "Distributed lock release failed. key=moamoa:lock:k1"
    }

    private fun <T> completedFuture(value: T): RFuture<T> {
        val future = mockk<RFuture<T>>()
        every { future.toCompletableFuture() } returns CompletableFuture.completedFuture(value)
        return future
    }

    private fun <T> failedFuture(cause: Throwable): RFuture<T> {
        val future = mockk<RFuture<T>>()
        val completableFuture = CompletableFuture<T>()
        completableFuture.completeExceptionally(cause)
        every { future.toCompletableFuture() } returns completableFuture
        return future
    }
}

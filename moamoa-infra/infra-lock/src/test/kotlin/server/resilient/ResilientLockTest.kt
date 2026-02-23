package server.resilient

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.context.annotation.Import
import server.config.ResilientLockProperties
import server.shared.lock.KeyedLock
import server.shared.lock.LockInfraException
import java.util.function.Supplier

class ResilientLockTest {

    @Test
    fun `redisson 성공 시 redisson만 사용한다`() = runTest {
        val redissonLock = mockk<KeyedLock>()
        val coroutineMutexLock = mockk<KeyedLock>()
        coEvery { redissonLock.withLock<String>("k1", any()) } coAnswers {
            secondArg<suspend () -> String>().invoke()
        }

        withContext(redissonLock, coroutineMutexLock) { keyedLock ->
            val result = keyedLock.withLock("k1") { "ok" }

            result shouldBe "ok"
            coVerify(exactly = 1) { redissonLock.withLock<String>("k1", any()) }
            coVerify(exactly = 0) { coroutineMutexLock.withLock<String>(any(), any()) }
        }
    }

    @Test
    fun `redisson 인프라 예외 발생 시 coroutine mutex로 폴백한다`() = runTest {
        val redissonLock = mockk<KeyedLock>()
        val coroutineMutexLock = mockk<KeyedLock>()
        coEvery { redissonLock.withLock<String>("k1", any()) } throws LockInfraException("redis down")
        coEvery { coroutineMutexLock.withLock<String>("k1", any()) } coAnswers {
            secondArg<suspend () -> String>().invoke()
        }

        withContext(redissonLock, coroutineMutexLock) { keyedLock ->
            val result = keyedLock.withLock("k1") { "local" }

            result shouldBe "local"
            coVerify(exactly = 1) { redissonLock.withLock<String>("k1", any()) }
            coVerify(exactly = 1) { coroutineMutexLock.withLock<String>("k1", any()) }
        }
    }

    @Test
    fun `degrade 상태에서는 probe 간격 전 redisson을 재시도하지 않는다`() = runTest {
        val redissonLock = mockk<KeyedLock>()
        val coroutineMutexLock = mockk<KeyedLock>()
        coEvery { redissonLock.withLock<String>("k1", any()) } throws LockInfraException("redis down")
        coEvery { coroutineMutexLock.withLock<String>("k1", any()) } coAnswers {
            secondArg<suspend () -> String>().invoke()
        }

        withContext(redissonLock, coroutineMutexLock) { keyedLock ->
            keyedLock.withLock("k1") { "local-1" }
            keyedLock.withLock("k1") { "local-2" }

            coVerify(exactly = 1) { redissonLock.withLock<String>("k1", any()) }
            coVerify(exactly = 2) { coroutineMutexLock.withLock<String>("k1", any()) }
        }
    }

    @Test
    fun `probe 성공 시 redisson으로 복귀한다`() = runTest {
        val redissonLock = mockk<KeyedLock>()
        val coroutineMutexLock = mockk<KeyedLock>()
        coEvery { redissonLock.withLock<String>("k1", any()) } throws LockInfraException("redis down") andThen "redis-recovered" andThen "redis-next"
        coEvery { coroutineMutexLock.withLock<String>("k1", any()) } coAnswers {
            secondArg<suspend () -> String>().invoke()
        }

        withContext(redissonLock, coroutineMutexLock, probeIntervalMs = 150L) { keyedLock ->
            val degraded = keyedLock.withLock("k1") { "local" }
            Thread.sleep(200L)
            val recovered = keyedLock.withLock("k1") { "ignored" }
            val healthy = keyedLock.withLock("k1") { "ignored" }

            degraded shouldBe "local"
            recovered shouldBe "redis-recovered"
            healthy shouldBe "redis-next"
            coVerify(exactly = 3) { redissonLock.withLock<String>("k1", any()) }
            coVerify(exactly = 1) { coroutineMutexLock.withLock<String>("k1", any()) }
        }
    }

    @Test
    fun `fallback 실패 시 원래 인프라 예외를 suppressed로 보존한다`() = runTest {
        val redissonLock = mockk<KeyedLock>()
        val coroutineMutexLock = mockk<KeyedLock>()
        val infraEx = LockInfraException("redis down")
        coEvery { redissonLock.withLock<String>("k1", any()) } throws infraEx
        coEvery { coroutineMutexLock.withLock<String>("k1", any()) } throws IllegalStateException("local fail")

        withContext(redissonLock, coroutineMutexLock) { keyedLock ->
            val ex = shouldThrow<IllegalStateException> {
                keyedLock.withLock("k1") { "ignored" }
            }

            ex.message shouldBe "local fail"
            containsInfraException(ex, "redis down") shouldBe true
        }
    }

    @Test
    fun `비인프라 예외는 fallback 트리거가 아니다`() = runTest {
        val redissonLock = mockk<KeyedLock>()
        val coroutineMutexLock = mockk<KeyedLock>()
        coEvery { redissonLock.withLock<String>("k1", any()) } throws IllegalStateException("business fail")

        withContext(redissonLock, coroutineMutexLock) { keyedLock ->
            val ex = shouldThrow<IllegalStateException> {
                keyedLock.withLock("k1") { "ignored" }
            }

            ex.message shouldBe "business fail"
            coVerify(exactly = 0) { coroutineMutexLock.withLock<String>(any(), any()) }
        }
    }

    @Test
    fun `withGlobalLock도 AOP 대상으로 동작한다`() = runTest {
        val redissonLock = mockk<KeyedLock>()
        val coroutineMutexLock = mockk<KeyedLock>()
        coEvery { redissonLock.withGlobalLock<String>(any()) } coAnswers {
            firstArg<suspend () -> String>().invoke()
        }

        withContext(redissonLock, coroutineMutexLock) { keyedLock ->
            val result = keyedLock.withGlobalLock { "global-ok" }

            result shouldBe "global-ok"
            coVerify(exactly = 1) { redissonLock.withGlobalLock<String>(any()) }
            coVerify(exactly = 0) { coroutineMutexLock.withGlobalLock<String>(any()) }
        }
    }

    private inline fun withContext(
        redissonLock: KeyedLock,
        coroutineMutexLock: KeyedLock,
        probeIntervalMs: Long = 30_000L,
        block: (keyedLock: KeyedLock) -> Unit,
    ) {
        AnnotationConfigApplicationContext().use { context ->
            context.registerBean("redissonLock", KeyedLock::class.java, Supplier { redissonLock })
            context.registerBean("coroutineMutexLock", KeyedLock::class.java, Supplier { coroutineMutexLock })
            context.registerBean(
                ResilientLockProperties::class.java,
                Supplier { ResilientLockProperties(recoveryProbeIntervalMs = probeIntervalMs) },
            )
            context.register(TestAopConfig::class.java)
            context.refresh()

            block(context.getBean(KeyedLock::class.java))
        }
    }

    @Configuration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    @Import(ResilientLock::class, ResilientLockAspect::class)
    internal class TestAopConfig

    private fun containsInfraException(throwable: Throwable?, message: String): Boolean {
        if (throwable == null) return false
        if (throwable is LockInfraException && throwable.message == message) return true
        if (containsInfraException(throwable.cause, message)) return true
        return throwable.suppressed.any { containsInfraException(it, message) }
    }
}

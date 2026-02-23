package server.infra.cache

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import server.shared.lock.KeyedLock
import test.UnitTest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class WarmupCoordinatorTest : UnitTest() {
    @Test
    fun `동일 키로 동시에 요청되면 단 한 번만 실행된다`() = runTest {
        val coordinator = createCoordinator(testScheduler)
        val executed = AtomicInteger(0)

        coordinator.launchIfAbsent("warmup:key") {
            executed.incrementAndGet()
        }
        coordinator.launchIfAbsent("warmup:key") {
            executed.incrementAndGet()
        }

        advanceUntilIdle()

        executed.get() shouldBe 1
    }

    @Test
    fun `첫 실행 완료 후에는 동일 키를 다시 실행할 수 있다`() = runTest {
        val coordinator = createCoordinator(testScheduler)
        val executed = AtomicInteger(0)

        coordinator.launchIfAbsent("warmup:key") {
            executed.incrementAndGet()
        }
        advanceUntilIdle()

        coordinator.launchIfAbsent("warmup:key") {
            executed.incrementAndGet()
        }
        advanceUntilIdle()

        executed.get() shouldBe 2
    }

    @Test
    fun `실행 중 예외가 발생해도 in-flight 키는 정리된다`() = runTest {
        val coordinator = createCoordinator(testScheduler)
        val executed = AtomicInteger(0)

        coordinator.launchIfAbsent("warmup:key") {
            error("boom")
        }
        advanceUntilIdle()

        coordinator.launchIfAbsent("warmup:key") {
            executed.incrementAndGet()
        }
        advanceUntilIdle()

        executed.get() shouldBe 1
    }

    private fun createCoordinator(scheduler: TestCoroutineScheduler): WarmupCoordinator {
        val scope = CoroutineScope(
            SupervisorJob() +
                StandardTestDispatcher(scheduler) +
                CoroutineExceptionHandler { _, _ -> }
        )
        return WarmupCoordinator(
            keyedLock = TestKeyedLock(),
            warmupScope = scope,
        )
    }

    private class TestKeyedLock : KeyedLock {
        private val mutexByKey = ConcurrentHashMap<String, Mutex>()

        override suspend fun <T> withLock(key: String, block: suspend () -> T): T {
            val mutex = mutexByKey.computeIfAbsent(key) { Mutex() }
            return mutex.withLock { block() }
        }
    }
}

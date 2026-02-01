package server.global.lock

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import test.UnitTest

class KeyedMutexTest : UnitTest() {
    @Test
    fun `같은 키는 동시에 진입하지 못한다`() = runTest {
        val mutex = KeyedMutex()
        val firstEntered = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        val secondEntered = CompletableDeferred<Unit>()

        val firstJob = launch {
            mutex.withLock("key") {
                firstEntered.complete(Unit)
                releaseFirst.await()
            }
        }
        val secondJob = launch {
            mutex.withLock("key") {
                secondEntered.complete(Unit)
            }
        }

        firstEntered.await()
        secondEntered.isCompleted shouldBe false
        releaseFirst.complete(Unit)
        secondEntered.await()

        firstJob.join()
        secondJob.join()
    }

    @Test
    fun `다른 키는 동시에 진입할 수 있다`() = runTest {
        val mutex = KeyedMutex()
        val firstEntered = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        val secondEntered = CompletableDeferred<Unit>()

        val firstJob = launch {
            mutex.withLock("key-1") {
                firstEntered.complete(Unit)
                releaseFirst.await()
            }
        }
        val secondJob = launch {
            firstEntered.await()
            mutex.withLock("key-2") {
                secondEntered.complete(Unit)
            }
        }

        firstEntered.await()
        withTimeout(1_000) { secondEntered.await() }
        releaseFirst.complete(Unit)

        firstJob.join()
        secondJob.join()
    }
}

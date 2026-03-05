package server.lock

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import server.lock.ReentrantKeyedLock
import test.UnitTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class ReentrantKeyedLockTest : UnitTest() {
    @Test
    fun `같은 키는 동시에 진입하지 못한다`() {
        val keyedLock = ReentrantKeyedLock()
        val firstEntered = CountDownLatch(1)
        val releaseFirst = CountDownLatch(1)
        val secondEntered = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val firstFuture = executor.submit {
                keyedLock.withLock("key") {
                    firstEntered.countDown()
                    releaseFirst.await(2, TimeUnit.SECONDS)
                }
            }

            firstEntered.await(1, TimeUnit.SECONDS) shouldBe true

            val secondFuture = executor.submit {
                keyedLock.withLock("key") {
                    secondEntered.countDown()
                }
            }

            secondEntered.count shouldBe 1L
            releaseFirst.countDown()
            secondEntered.await(1, TimeUnit.SECONDS) shouldBe true

            firstFuture.get(2, TimeUnit.SECONDS)
            secondFuture.get(2, TimeUnit.SECONDS)
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `다른 키는 동시에 진입할 수 있다`() {
        val keyedLock = ReentrantKeyedLock()
        val firstEntered = CountDownLatch(1)
        val releaseFirst = CountDownLatch(1)
        val secondEntered = AtomicBoolean(false)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val firstFuture = executor.submit {
                keyedLock.withLock("key-1") {
                    firstEntered.countDown()
                    releaseFirst.await(2, TimeUnit.SECONDS)
                }
            }
            val secondFuture = executor.submit {
                firstEntered.await(1, TimeUnit.SECONDS)
                keyedLock.withLock("key-2") {
                    secondEntered.set(true)
                }
            }

            firstEntered.await(1, TimeUnit.SECONDS) shouldBe true
            Thread.sleep(100L)
            secondEntered.get() shouldBe true
            releaseFirst.countDown()

            firstFuture.get(2, TimeUnit.SECONDS)
            secondFuture.get(2, TimeUnit.SECONDS)
        } finally {
            executor.shutdownNow()
        }
    }
}

package server.resilient

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import server.config.ResilientLockProperties
import server.shared.lock.KeyedLock
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Component
internal class ResilientKeyedLockRouter(
    @param:Qualifier("redissonLock")
    private val redissonLock: KeyedLock,
    @param:Qualifier("coroutineMutexLock")
    private val coroutineMutexLock: KeyedLock,
    private val properties: ResilientLockProperties,
) {
    private val logger = KotlinLogging.logger {}
    private val activeLock = AtomicReference<KeyedLock>(redissonLock)
    private val switchLock = ReentrantLock()

    @Volatile
    private var lastProbeAtMillis: Long = 0L

    fun current(): KeyedLock = activeLock.get()

    fun isCoroutineMutex(target: KeyedLock): Boolean = target === coroutineMutexLock

    fun maybePromoteToRedis(): Boolean {
        if (activeLock.get() !== coroutineMutexLock) {
            return false
        }

        val now = nowMillis()
        if (now - lastProbeAtMillis < recoveryProbeIntervalMillis()) {
            return false
        }

        return switchLock.withLock {
            if (activeLock.get() !== coroutineMutexLock) {
                return@withLock false
            }

            val current = nowMillis()
            if (current - lastProbeAtMillis < recoveryProbeIntervalMillis()) {
                return@withLock false
            }

            lastProbeAtMillis = current
            activeLock.set(redissonLock)
            true
        }
    }

    fun switchToCoroutineMutex(ex: Throwable): KeyedLock {
        switchLock.withLock {
            if (activeLock.get() === redissonLock) {
                activeLock.set(coroutineMutexLock)
                lastProbeAtMillis = nowMillis()
                logger.warn(ex) {
                    "Lock degraded. fallback=coroutineMutex reason=${ex.javaClass.simpleName}"
                }
            }
        }
        return coroutineMutexLock
    }

    private fun recoveryProbeIntervalMillis(): Long = properties.recoveryProbeIntervalMs.coerceAtLeast(1)

    private fun nowMillis(): Long = System.currentTimeMillis()
}

package server.cache

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import server.config.ResilientCacheProperties
import server.shared.cache.CacheMemory
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Component
internal class ResilientCacheRouter(
    @param:Qualifier("redisCacheMemory")
    private val redisCacheMemory: CacheMemory,
    @param:Qualifier("caffeineCacheMemory")
    private val caffeineCacheMemory: CacheMemory,
    private val properties: ResilientCacheProperties,
) {
    private val logger = KotlinLogging.logger {}
    private val activeCacheMemory = AtomicReference<CacheMemory>(redisCacheMemory)
    private val switchLock = ReentrantLock()

    @Volatile
    private var lastProbeAtMillis: Long = 0L

    fun current(): CacheMemory = activeCacheMemory.get()

    fun isCaffeine(target: CacheMemory): Boolean = target === caffeineCacheMemory

    fun maybePromoteToRedis(): Boolean {
        if (activeCacheMemory.get() !== caffeineCacheMemory) {
            return false
        }

        val now = nowMillis()
        if (now - lastProbeAtMillis < recoveryProbeIntervalMillis()) {
            return false
        }

        return switchLock.withLock {
            if (activeCacheMemory.get() !== caffeineCacheMemory) {
                return@withLock false
            }

            val current = nowMillis()
            if (current - lastProbeAtMillis < recoveryProbeIntervalMillis()) {
                return@withLock false
            }

            lastProbeAtMillis = current
            activeCacheMemory.set(redisCacheMemory)
            true
        }
    }

    fun switchToCaffeine(ex: Throwable): CacheMemory {
        switchLock.withLock {
            if (activeCacheMemory.get() === redisCacheMemory) {
                activeCacheMemory.set(caffeineCacheMemory)
                lastProbeAtMillis = nowMillis()
                logger.warn(ex) { "Cache degraded. fallback=caffeine reason=${ex.javaClass.simpleName}" }
            }
        }
        return caffeineCacheMemory
    }

    private fun recoveryProbeIntervalMillis(): Long = properties.recoveryProbeIntervalMs.coerceAtLeast(1)

    private fun nowMillis(): Long = System.currentTimeMillis()
}

package server.messaging.health

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.RedisConnectionFailureException
import org.springframework.data.redis.RedisSystemException
import org.springframework.data.redis.core.RedisCallback
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import server.config.RedisHealthProperties
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

@Component
internal class RedisHealthStateManager(
    private val properties: RedisHealthProperties,
    @param:Qualifier("schedulerScope")
    private val schedulerScope: CoroutineScope,
    @param:Qualifier("streamStringRedisTemplate")
    private val redis: StringRedisTemplate,
    private val recoveryActionRunner: RedisRecoveryActionRunner,
) {
    private val logger = KotlinLogging.logger {}

    private enum class State {
        ACTIVE,
        DEGRADED,
    }

    private val state = AtomicReference(State.ACTIVE)
    private val recoveryLoopRunning = AtomicBoolean(false)

    suspend fun <T> runSafe(block: suspend () -> T): Result<T> {
        if (isDegraded()) return Result.failure(IllegalStateException("redis is degraded"))

        return try {
            Result.success(block())
        } catch (e: Exception) {
            if (isRedisFailure(e)) {
                enterDegraded(e)
                Result.failure(e)
            } else {
                throw e
            }
        }
    }

    fun isDegraded(): Boolean = state.get() == State.DEGRADED

    suspend fun tryRecover(): Boolean {
        if (!isDegraded()) return true

        val recovered = runCatching { ping() }
            .onFailure { e ->
                logger.debug(e) { "redis health probe failed" }
            }
            .isSuccess

        if (!recovered) return false

        val changed = state.compareAndSet(State.DEGRADED, State.ACTIVE)
        if (changed) {
            logger.warn { "redis recovered" }
            runCatching {
                recoveryActionRunner.runAll()
            }.onFailure { e ->
                logger.warn(e) { "redis recovery actions failed" }
            }
        }

        return true
    }

    private fun enterDegraded(cause: Throwable) {
        val changed = state.compareAndSet(State.ACTIVE, State.DEGRADED)
        if (!changed) return

        logger.warn(cause) { "redis degraded" }
        startRecoveryLoopIfNeeded()
    }

    private fun startRecoveryLoopIfNeeded() {
        if (!recoveryLoopRunning.compareAndSet(false, true)) return

        schedulerScope.launch {
            try {
                delay(properties.pauseOnFailureMs.coerceAtLeast(1L))

                while (isDegraded()) {
                    if (tryRecover()) break
                    delay(properties.recoveryProbeIntervalMs.coerceAtLeast(1L))
                }
            } finally {
                recoveryLoopRunning.set(false)
                if (isDegraded()) {
                    startRecoveryLoopIfNeeded()
                }
            }
        }
    }

    private suspend fun ping(): String = withContext(Dispatchers.IO) {
        redis.execute(RedisCallback { connection -> connection.ping() })
            ?: error("redis ping returned null")
    }

    private fun isRedisFailure(exception: Throwable): Boolean =
        exception is RedisConnectionFailureException || exception is RedisSystemException
}

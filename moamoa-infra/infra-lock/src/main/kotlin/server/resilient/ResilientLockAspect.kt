package server.resilient

import io.github.oshai.kotlinlogging.KotlinLogging
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import server.config.ResilientLockProperties
import server.shared.lock.KeyedLock
import server.shared.lock.LockInfraException
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Aspect
@Component
@Suppress("UNCHECKED_CAST")
internal class ResilientLockAspect(
    @param:Qualifier("redissonLock")
    private val redissonLock: KeyedLock,
    @param:Qualifier("coroutineMutexLock")
    private val coroutineMutexLock: KeyedLock,
    private val properties: ResilientLockProperties,
) {
    private val logger = KotlinLogging.logger {}
    private val activeLock = AtomicReference<KeyedLock>(redissonLock)
    private val switchLock = ReentrantLock()
    private val methodCache = ConcurrentHashMap<MethodCacheKey, Method>()

    @Volatile
    private var lastProbeAtMillis: Long = 0L

    @Around(
        "bean(resilientLock) && " +
            "execution(* server.shared.lock.KeyedLock.*(..)) && " +
            "args(.., kotlin.coroutines.Continuation)"
    )
    fun around(joinPoint: ProceedingJoinPoint): Any? {
        val method = (joinPoint.signature as? MethodSignature)?.method ?: return joinPoint.proceed()

        val args = joinPoint.args ?: emptyArray()
        val promotedToRedis = maybePromoteToRedis()
        if (activeLock.get() === coroutineMutexLock) {
            return invoke(coroutineMutexLock, method, args)
        }

        return try {
            val proceeded = joinPoint.proceed()

            if (proceeded !is Mono<*>) {
                throw IllegalStateException("Expected Mono from suspend KeyedLock method")
            }

            val mono = proceeded as Mono<Any?>
            mono
                .doOnSuccess {
                    if (promotedToRedis) {
                        logger.warn { "Lock recovered. backend=redis" }
                    }
                }
                .onErrorResume { ex ->
                    if (ex !is LockInfraException) {
                        return@onErrorResume Mono.error(ex)
                    }
                    switchToCoroutineMutex(ex)
                    invokeAsMono(coroutineMutexLock, method, args, ex)
                }
        } catch (ex: Throwable) {
            if (ex !is LockInfraException) {
                throw ex
            }
            switchToCoroutineMutex(ex)
            try {
                invoke(coroutineMutexLock, method, args)
            } catch (fallbackException: Throwable) {
                fallbackException.addSuppressed(ex)
                throw fallbackException
            }
        }
    }

    private fun maybePromoteToRedis(): Boolean {
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

    private fun switchToCoroutineMutex(ex: Throwable) {
        switchLock.withLock {
            if (activeLock.get() === redissonLock) {
                activeLock.set(coroutineMutexLock)
                lastProbeAtMillis = nowMillis()
                logger.warn(ex) {
                    "Lock degraded. fallback=coroutineMutex reason=${ex.javaClass.simpleName}"
                }
            }
        }
    }

    private fun invoke(target: KeyedLock, method: Method, args: Array<Any?>): Any? {
        val targetMethod = resolveMethod(target, method)
        return try {
            targetMethod.invoke(target, *args)
        } catch (ex: InvocationTargetException) {
            throw ex.targetException ?: ex
        }
    }

    private fun invokeAsMono(
        target: KeyedLock,
        method: Method,
        args: Array<Any?>,
        originalException: Throwable,
    ): Mono<Any?> {
        return try {
            val result = invoke(target, method, args)
            when (result) {
                is Mono<*> -> result as Mono<Any?>
                else -> Mono.justOrEmpty(result)
            }
        } catch (fallbackException: Throwable) {
            fallbackException.addSuppressed(originalException)
            Mono.error(fallbackException)
        }
    }

    private fun resolveMethod(target: KeyedLock, sourceMethod: Method): Method {
        val key = MethodCacheKey(
            targetClass = target.javaClass,
            methodName = sourceMethod.name,
            parameterTypes = sourceMethod.parameterTypes.toList(),
        )

        return methodCache.getOrPut(key) {
            target.javaClass.methods.firstOrNull { candidate ->
                candidate.name == sourceMethod.name &&
                    candidate.parameterTypes.contentEquals(sourceMethod.parameterTypes)
            } ?: sourceMethod
        }
    }

    private fun recoveryProbeIntervalMillis(): Long = properties.recoveryProbeIntervalMs.coerceAtLeast(1)

    private fun nowMillis(): Long = System.currentTimeMillis()

    private data class MethodCacheKey(
        val targetClass: Class<*>,
        val methodName: String,
        val parameterTypes: List<Class<*>>,
    )
}

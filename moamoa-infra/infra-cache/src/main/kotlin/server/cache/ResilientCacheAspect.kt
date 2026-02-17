package server.cache

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import server.config.ResilientCacheProperties
import server.shared.cache.CacheMemory
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Function
import kotlin.concurrent.withLock

@Aspect
@Component
@Suppress("UNCHECKED_CAST")
internal class ResilientCacheAspect(
    @param:Qualifier("redisCacheMemory")
    private val redisCacheMemory: CacheMemory,
    @param:Qualifier("caffeineCacheMemory")
    private val caffeineCacheMemory: CacheMemory,
    private val properties: ResilientCacheProperties,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val activeCacheMemory = AtomicReference<CacheMemory>(redisCacheMemory)
    private val switchLock = ReentrantLock()
    private val methodCache = ConcurrentHashMap<MethodCacheKey, Method>()

    @Volatile
    private var lastProbeAtMillis: Long = 0L

    @Around(
        "bean(resilientCacheMemoryProxy) && " +
            "execution(* server.shared.cache.CacheMemory.*(..)) && " +
            "args(.., kotlin.coroutines.Continuation)"
    )
    fun around(joinPoint: ProceedingJoinPoint): Any? {
        val method = (joinPoint.signature as? MethodSignature)?.method ?: return joinPoint.proceed()

        val args = joinPoint.args ?: emptyArray()
        val promotedToRedis = maybePromoteToRedis()
        val current = activeCacheMemory.get()
        if (current === caffeineCacheMemory) {
            return invoke(caffeineCacheMemory, method, args)
        }

        return try {
            val proceeded = joinPoint.proceed()

            if (proceeded !is Mono<*>) {
                throw IllegalStateException("Expected Mono from suspend CacheMemory method")
            }

            val mono = proceeded as Mono<Any?>
            mono
                .doOnSuccess {
                    if (promotedToRedis) {
                        logger.warn("Cache recovered. backend=redis")
                    }
                }
                .onErrorResume { ex ->
                    switchToCaffeine(ex)
                    invokeAsMono(caffeineCacheMemory, method, args, ex)
                }
        } catch (ex: Throwable) {
            switchToCaffeine(ex)
            invoke(caffeineCacheMemory, method, args)
        }
    }

    private fun maybePromoteToRedis(): Boolean {
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

    private fun switchToCaffeine(ex: Throwable) {
        switchLock.withLock {
            if (activeCacheMemory.get() === redisCacheMemory) {
                activeCacheMemory.set(caffeineCacheMemory)
                lastProbeAtMillis = nowMillis()
                logger.warn("Cache degraded. fallback=caffeine reason=${ex.javaClass.simpleName}", ex)
            }
        }
    }

    private fun invoke(target: CacheMemory, method: Method, args: Array<Any?>): Any? {
        val targetMethod = resolveMethod(target, method)
        return try {
            targetMethod.invoke(target, *args)
        } catch (ex: InvocationTargetException) {
            throw ex.targetException ?: ex
        }
    }

    private fun invokeAsMono(
        target: CacheMemory,
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

    private fun resolveMethod(target: CacheMemory, sourceMethod: Method): Method {
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

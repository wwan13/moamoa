package server.cache

import io.github.oshai.kotlinlogging.KotlinLogging
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.stereotype.Component

@Aspect
@Component
@Suppress("UNCHECKED_CAST")
internal class ResilientCacheAspect(
    private val resilientCacheRouter: ResilientCacheRouter,
    private val resilientCacheMethodInvoker: ResilientCacheMethodInvoker,
) {
    private val logger = KotlinLogging.logger {}

    @Around(
        "bean(resilientCacheMemoryProxy) && " +
            "execution(* server.shared.cache.CacheMemory.*(..))"
    )
    fun around(joinPoint: ProceedingJoinPoint): Any? {
        val method = (joinPoint.signature as? MethodSignature)?.method ?: return joinPoint.proceed()

        val args = joinPoint.args ?: emptyArray()
        val promotedToRedis = resilientCacheRouter.maybePromoteToRedis()
        val current = resilientCacheRouter.current()
        if (resilientCacheRouter.isCaffeine(current)) {
            return resilientCacheMethodInvoker.invoke(current, method, args)
        }

        return try {
            joinPoint.proceed().also {
                if (promotedToRedis) {
                    logger.warn { "Cache recovered. backend=redis" }
                }
            }
        } catch (ex: Throwable) {
            if (ex !is CacheInfraException) throw ex
            val fallbackTarget = resilientCacheRouter.switchToCaffeine(ex)
            resilientCacheMethodInvoker.invoke(fallbackTarget, method, args, ex)
        }
    }
}

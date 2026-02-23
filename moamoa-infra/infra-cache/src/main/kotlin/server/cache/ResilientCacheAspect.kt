package server.cache

import io.github.oshai.kotlinlogging.KotlinLogging
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import server.shared.cache.CacheInfraException
import server.shared.cache.CacheMemory

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
            "execution(* server.shared.cache.CacheMemory.*(..)) && " +
            "args(.., kotlin.coroutines.Continuation)"
    )
    fun around(joinPoint: ProceedingJoinPoint): Any? {
        val method = (joinPoint.signature as? MethodSignature)?.method ?: return joinPoint.proceed()

        val args = joinPoint.args ?: emptyArray()
        val promotedToRedis = resilientCacheRouter.maybePromoteToRedis()
        val current = resilientCacheRouter.current()
        if (resilientCacheRouter.isCaffeine(current)) {
            return resilientCacheMethodInvoker.invokeAsMono(current, method, args)
        }

        val mono = joinPoint.proceed() as Mono<Any?>
        return mono
            .doOnSuccess {
                if (promotedToRedis) {
                    logger.warn { "Cache recovered. backend=redis" }
                }
            }
            .onErrorResume { ex ->
                if (ex !is CacheInfraException) {
                    return@onErrorResume Mono.error(ex)
                }
                val fallbackTarget = resilientCacheRouter.switchToCaffeine(ex)
                resilientCacheMethodInvoker.invokeAsMono(fallbackTarget, method, args, ex)
            }
    }
}

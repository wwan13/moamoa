package server.resilient

import io.github.oshai.kotlinlogging.KotlinLogging
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import server.shared.lock.LockInfraException

@Aspect
@Component
@Suppress("UNCHECKED_CAST")
internal class ResilientKeyedLockAspect(
    private val resilientKeyedLockRouter: ResilientKeyedLockRouter,
    private val resilientKeyedLockMethodInvoker: ResilientKeyedLockMethodInvoker,
) {
    private val logger = KotlinLogging.logger {}

    @Around(
        "bean(resilientKeyedLock) && " +
            "execution(* server.shared.lock.KeyedLock.*(..)) && " +
            "args(.., kotlin.coroutines.Continuation)"
    )
    fun around(joinPoint: ProceedingJoinPoint): Any? {
        val method = (joinPoint.signature as? MethodSignature)?.method ?: return joinPoint.proceed()

        val args = joinPoint.args ?: emptyArray()
        val promotedToRedis = resilientKeyedLockRouter.maybePromoteToRedis()
        val current = resilientKeyedLockRouter.current()
        if (resilientKeyedLockRouter.isCoroutineMutex(current)) {
            return resilientKeyedLockMethodInvoker.invoke(current, method, args)
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
                    val fallbackTarget = resilientKeyedLockRouter.switchToCoroutineMutex(ex)
                    resilientKeyedLockMethodInvoker.invokeAsMono(fallbackTarget, method, args, ex)
                }
        } catch (ex: Throwable) {
            if (ex !is LockInfraException) {
                throw ex
            }
            val fallbackTarget = resilientKeyedLockRouter.switchToCoroutineMutex(ex)
            try {
                resilientKeyedLockMethodInvoker.invoke(fallbackTarget, method, args)
            } catch (fallbackException: Throwable) {
                fallbackException.addSuppressed(ex)
                throw fallbackException
            }
        }
    }
}

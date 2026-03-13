package server.lock

import io.github.oshai.kotlinlogging.KotlinLogging
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.stereotype.Component

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
            "execution(* server.shared.lock.KeyedLock.*(..))"
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
            joinPoint.proceed().also {
                if (promotedToRedis) {
                    logger.warn { "Lock recovered. backend=redis" }
                }
            }
        } catch (ex: Throwable) {
            if (ex !is LockInfraException) {
                throw ex
            }
            val fallbackTarget = resilientKeyedLockRouter.switchToCoroutineMutex(ex)
            resilientKeyedLockMethodInvoker.invoke(fallbackTarget, method, args, ex)
        }
    }
}

package server.global.logging

import io.github.oshai.kotlinlogging.KotlinLogging
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.stereotype.Component
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.time.TimeMark
import kotlin.time.TimeSource

@Aspect
@Component
class CacheLoggingAspect {
    private val logger = KotlinLogging.logger {}

    @Around("within(server.infra.cache..*)")
    fun aroundCache(joinPoint: ProceedingJoinPoint): Any? {
        val signature = joinPoint.signature.toShortString()
        val method = (joinPoint.signature as? MethodSignature)?.method
        val isSuspendMethod = method?.parameterTypes?.lastOrNull() == Continuation::class.java
        if (!isSuspendMethod) {
            return joinPoint.proceed()
        }

        val args = joinPoint.args
        val continuation = args.lastOrNull() as? Continuation<Any?>
        if (continuation == null) {
            return joinPoint.proceed()
        }

        val mark = TimeSource.Monotonic.markNow()
        val context = RequestLogContextHolder.fromContinuation(continuation)
        val wrappedContinuation = LoggingContinuation(
            delegate = continuation,
            signature = signature,
            requestContext = context,
            mark = mark
        )
        val newArgs = args.copyOf().apply { this[lastIndex] = wrappedContinuation }
        val result = try {
            joinPoint.proceed(newArgs)
        } catch (error: Throwable) {
            logFailure(signature, context, mark.elapsedNow().inWholeMilliseconds, error)
            throw error
        }

        if (result !== COROUTINE_SUSPENDED) {
            logSuccess(signature, context, mark.elapsedNow().inWholeMilliseconds)
        }
        return result
    }

    private fun logSuccess(signature: String, context: RequestLogContext?, latencyMs: Long) {
        logger.infoWithTraceId(context?.traceId) {
            "[REDIS] result=SUCCESS call=$signature target=Redis latencyMs=$latencyMs retry=0 timeout=false userId=${context?.userId ?: "NONE"}"
        }
    }

    private fun logFailure(
        signature: String,
        context: RequestLogContext?,
        latencyMs: Long,
        error: Throwable
    ) {
        val errorCode = error::class.simpleName ?: "UnknownException"
        val errorSummary = error.message?.replace(Regex("\\s+"), " ")?.take(180) ?: "-"
        logger.warnWithTraceId(context?.traceId, error) {
            "[REDIS] result=FAIL call=$signature target=Redis latencyMs=$latencyMs retry=0 timeout=false errorCode=$errorCode errorSummary=$errorSummary userId=${context?.userId ?: "NONE"}"
        }
    }

    private inner class LoggingContinuation(
        private val delegate: Continuation<Any?>,
        private val signature: String,
        private val requestContext: RequestLogContext?,
        private val mark: TimeMark,
    ) : Continuation<Any?> {
        override val context = delegate.context

        override fun resumeWith(result: Result<Any?>) {
            val latencyMs = mark.elapsedNow().inWholeMilliseconds
            result
                .onSuccess { logSuccess(signature, this@LoggingContinuation.requestContext, latencyMs) }
                .onFailure { logFailure(signature, this@LoggingContinuation.requestContext, latencyMs, it) }
            delegate.resumeWith(result)
        }
    }
}

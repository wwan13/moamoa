package server.global.logging

import io.github.oshai.kotlinlogging.KotlinLogging
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.stereotype.Component
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.time.TimeMark
import kotlin.time.TimeSource

@Aspect
@Component
class RepositoryLoggingAspect {
    private val logger = KotlinLogging.logger {}

    @Around("this(org.springframework.data.repository.Repository)")
    fun aroundRepository(joinPoint: ProceedingJoinPoint): Any? {
        val signature = joinPoint.signature.toShortString()
        val method = (joinPoint.signature as? MethodSignature)?.method
        val isSuspendMethod = method?.parameterTypes?.lastOrNull() == Continuation::class.java
        if (!isSuspendMethod) {
            return measureBlocking(signature) { joinPoint.proceed() }
        }

        val args = joinPoint.args
        val continuation = args.lastOrNull() as? Continuation<Any?>
        if (continuation == null) {
            return measureBlocking(signature) { joinPoint.proceed() }
        }

        val mark = TimeSource.Monotonic.markNow()
        val context = RequestLogContextHolder.fromContinuation(continuation)
        val wrappedContinuation = LoggingContinuation(
            delegate = continuation,
            signature = signature,
            requestContext = context,
            markNanos = mark
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

    private inline fun measureBlocking(signature: String, block: () -> Any?): Any? {
        val mark = TimeSource.Monotonic.markNow()
        val context: RequestLogContext? = null
        return try {
            block().also {
                logSuccess(signature, context, mark.elapsedNow().inWholeMilliseconds)
            }
        } catch (error: Throwable) {
            logFailure(signature, context, mark.elapsedNow().inWholeMilliseconds, error)
            throw error
        }
    }

    private fun logSuccess(signature: String, context: RequestLogContext?, latencyMs: Long) {
        if (shouldSkipSuccessLog(signature, context)) return
        logger.infoWithTraceId(context?.traceId) {
            "[DB] result=SUCCESS call=$signature target=DB latencyMs=$latencyMs retry=0 timeout=false userId=${context?.userId ?: "NONE"}"
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
            "[DB] result=FAIL call=$signature target=DB latencyMs=$latencyMs retry=0 timeout=false errorCode=$errorCode errorSummary=$errorSummary userId=${context?.userId ?: "NONE"}"
        }
    }

    private fun shouldSkipSuccessLog(signature: String, context: RequestLogContext?): Boolean {
        if (context != null) return false
        return signature.startsWith("EventOutboxRepository.findUnpublished")
    }

    private inner class LoggingContinuation(
        private val delegate: Continuation<Any?>,
        private val signature: String,
        private val requestContext: RequestLogContext?,
        private val markNanos: TimeMark
    ) : Continuation<Any?> {
        override val context: CoroutineContext
            get() = runCatching { delegate.context }.getOrDefault(EmptyCoroutineContext)

        override fun resumeWith(result: Result<Any?>) {
            val latencyMs = markNanos.elapsedNow().inWholeMilliseconds
            result
                .onSuccess { logSuccess(signature, this@LoggingContinuation.requestContext, latencyMs) }
                .onFailure { logFailure(signature, this@LoggingContinuation.requestContext, latencyMs, it) }
            delegate.resumeWith(result)
        }
    }
}

package server.global.logging

import io.github.oshai.kotlinlogging.KotlinLogging
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.stereotype.Component
import kotlin.time.TimeSource

@Aspect
@Component
class RepositoryLoggingAspect {
    private val logger = KotlinLogging.logger {}

    @Around("this(org.springframework.data.repository.Repository)")
    fun aroundRepository(joinPoint: ProceedingJoinPoint): Any? {
        val signature = joinPoint.signature.toShortString()
        return measureBlocking(signature) { joinPoint.proceed() }
    }

    private inline fun measureBlocking(signature: String, block: () -> Any?): Any? {
        val mark = TimeSource.Monotonic.markNow()
        val context = RequestLogContextHolder.current()
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
        logger.db.info(
            traceId = context?.traceId,
            "call" to signature,
            "latencyMs" to latencyMs,
            "success" to true,
        ) {
            "DB 호출이 성공했습니다"
        }
    }

    private fun logFailure(
        signature: String,
        context: RequestLogContext?,
        latencyMs: Long,
        error: Throwable
    ) {
        val errorCode = error::class.simpleName ?: "UnknownException"
        val errorSummary = error.message?.replace(Regex("\\s+"), " ")?.take(180) ?: "unknown"
        logger.db.warn(
            traceId = context?.traceId,
            throwable = error,
            "call" to signature,
            "latencyMs" to latencyMs,
            "success" to false,
            "errorType" to errorCode,
        ) {
            "DB 호출이 실패했습니다"
        }
        logger.errorType.warn(
            traceId = context?.traceId,
            throwable = error,
            "call" to signature,
            "errorType" to errorCode,
            "message" to errorSummary,
        ) {
            "DB 호출 오류가 발생했습니다"
        }
    }

    private fun shouldSkipSuccessLog(signature: String, context: RequestLogContext?): Boolean {
        if (context != null) return false
        return signature.startsWith("EventOutboxRepository.findUnpublished")
    }
}

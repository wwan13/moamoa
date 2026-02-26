package server.global.logging

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.TimeoutCancellationException
import org.springframework.stereotype.Component
import kotlin.time.TimeSource

@Component
class ExternalCallLogger {
    private val logger = KotlinLogging.logger {}

    suspend fun <T> execute(
        call: String,
        target: String,
        retry: Int = 0,
        timeout: Boolean = false,
        block: suspend () -> T
    ): T {
        val mark = TimeSource.Monotonic.markNow()
        val context = RequestLogContextHolder.current()
        val typedLogger = externalType(target)
        return runCatching { block() }
            .onSuccess {
                val latencyMs = mark.elapsedNow().inWholeMilliseconds
                typedLogger.info(
                    traceId = context?.traceId,
                    "call" to call,
                    "latencyMs" to latencyMs,
                    "success" to true,
                ) {
                    "외부 호출이 성공했습니다"
                }
            }
            .onFailure { error ->
                val latencyMs = mark.elapsedNow().inWholeMilliseconds
                val errorType = if (timeout || error is TimeoutCancellationException) {
                    "TimeoutCancellationException"
                } else {
                    error::class.simpleName ?: "UnknownException"
                }
                val errorSummary = error.message.sanitizeForLog()
                typedLogger.warn(
                    traceId = context?.traceId,
                    throwable = error,
                    "call" to call,
                    "latencyMs" to latencyMs,
                    "success" to false,
                    "errorType" to errorType,
                ) {
                    "외부 호출이 실패했습니다"
                }
                logger.errorType.warn(
                    traceId = context?.traceId,
                    throwable = error,
                    "call" to call,
                    "errorType" to errorType,
                    "message" to errorSummary,
                ) {
                    "외부 호출 오류가 발생했습니다"
                }
            }
            .getOrThrow()
    }

    private fun externalType(target: String): TypedLogger =
        when (target.uppercase()) {
            "DB" -> logger.db
            "REDIS" -> logger.redis
            else -> logger.api
        }

    private fun String?.sanitizeForLog(): String {
        if (this.isNullOrBlank()) return "-"
        return this.replace(Regex("\\s+"), " ").take(180)
    }
}

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
        val type = externalType(target)
        return runCatching { block() }
            .onSuccess {
                val latencyMs = mark.elapsedNow().inWholeMilliseconds
                logger.infoWithTraceId(context?.traceId) {
                    "[$type] result=SUCCESS call=$call target=$target latencyMs=$latencyMs retry=$retry timeout=$timeout userId=${context?.userId ?: "NONE"}"
                }
            }
            .onFailure { error ->
                val latencyMs = mark.elapsedNow().inWholeMilliseconds
                val isTimeout = timeout || error is TimeoutCancellationException
                val result = if (isTimeout) "TIMEOUT" else "FAIL"
                val errorCode = error::class.simpleName ?: "UnknownException"
                val errorSummary = error.message.sanitizeForLog()
                logger.warnWithTraceId(context?.traceId, error) {
                    "[$type] result=$result call=$call target=$target latencyMs=$latencyMs retry=$retry timeout=$isTimeout errorCode=$errorCode errorSummary=$errorSummary userId=${context?.userId ?: "NONE"}"
                }
            }
            .getOrThrow()
    }

    private fun externalType(target: String): String =
        when (target.uppercase()) {
            "DB" -> "DB"
            "REDIS" -> "REDIS"
            else -> "API"
        }

    private fun String?.sanitizeForLog(): String {
        if (this.isNullOrBlank()) return "-"
        return this.replace(Regex("\\s+"), " ").take(180)
    }
}

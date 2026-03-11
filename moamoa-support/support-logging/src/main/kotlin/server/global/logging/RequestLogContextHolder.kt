package server.global.logging

import org.slf4j.MDC
import java.util.UUID

object RequestLogContextHolder {
    private const val TRACE_ID_KEY = "traceId"
    private const val USER_ID_KEY = "userId"
    private const val CLIENT_IP_KEY = "clientIp"

    const val TRACE_ID_HEADER = "X-Trace-Id"
    const val TRACE_ID_ATTR = "traceId"
    const val USER_ID_ATTR = "userId"
    const val CLIENT_IP_ATTR = "clientIp"

    const val SYSTEM_TRACE_ID = "SYSTEM"

    fun normalizeTraceId(rawTraceId: String?): String {
        val normalized = rawTraceId?.trim()
        if (normalized.isNullOrBlank()) {
            return UUID.randomUUID().toString().substring(0, 8)
        }

        return runCatching { UUID.fromString(normalized) }
            .getOrNull()
            ?.toString()
            ?.substring(0, 8)
            ?: normalized
    }

    fun current(): RequestLogContext? {
        val traceId = MDC.get(TRACE_ID_KEY) ?: return null
        val userId = MDC.get(USER_ID_KEY)?.toLongOrNull()
        val clientIp = MDC.get(CLIENT_IP_KEY)
        return RequestLogContext(traceId = traceId, userId = userId, clientIp = clientIp)
    }

    fun <T> withTraceId(traceId: String?, block: () -> T): T {
        val normalized = normalizeTraceId(traceId)
        return withMdcValue(TRACE_ID_KEY, normalized, block)
    }

    fun <T> withContext(context: RequestLogContext, block: () -> T): T {
        val normalizedTraceId = normalizeTraceId(context.traceId)
        val userId = context.userId?.toString()
        val clientIp = context.clientIp
        return withMdcValue(TRACE_ID_KEY, normalizedTraceId) {
            withMdcValue(USER_ID_KEY, userId) {
                withMdcValue(CLIENT_IP_KEY, clientIp, block)
            }
        }
    }

    private fun <T> withMdcValue(key: String, value: String?, block: () -> T): T {
        val previous = MDC.get(key)
        if (value != null) {
            MDC.put(key, value)
        } else {
            MDC.remove(key)
        }
        return try {
            block()
        } finally {
            if (previous != null) {
                MDC.put(key, previous)
            } else {
                MDC.remove(key)
            }
        }
    }
}

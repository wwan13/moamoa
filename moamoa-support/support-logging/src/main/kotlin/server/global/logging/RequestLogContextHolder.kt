package server.global.logging

import org.slf4j.MDC
import java.util.UUID

object RequestLogContextHolder {
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
        val traceId = MDC.get("traceId") ?: return null
        val userId = MDC.get("userId")?.toLongOrNull()
        val clientIp = MDC.get("clientIp")
        return RequestLogContext(traceId = traceId, userId = userId, clientIp = clientIp)
    }

    inline fun <T> withTraceId(traceId: String?, block: () -> T): T {
        val normalized = normalizeTraceId(traceId)
        val closeable = MDC.putCloseable("traceId", normalized)
        return try {
            block()
        } finally {
            closeable.close()
        }
    }

    inline fun <T> withContext(context: RequestLogContext, block: () -> T): T {
        val traceCloseable = MDC.putCloseable("traceId", normalizeTraceId(context.traceId))
        val userCloseable = context.userId?.let { MDC.putCloseable("userId", it.toString()) }
        val clientIpCloseable = context.clientIp?.let { MDC.putCloseable("clientIp", it) }
        return try {
            block()
        } finally {
            clientIpCloseable?.close()
            userCloseable?.close()
            traceCloseable.close()
        }
    }
}

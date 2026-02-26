package server.global.logging

import kotlinx.coroutines.reactor.ReactorContext
import org.slf4j.MDC
import org.springframework.web.server.ServerWebExchange
import reactor.util.context.Context
import kotlin.coroutines.Continuation
import kotlin.coroutines.coroutineContext

object RequestLogContextHolder {
    const val TRACE_ID_HEADER = "X-Trace-Id"
    const val TRACE_ID_ATTR = "traceId"
    const val USER_ID_ATTR = "userId"
    const val CLIENT_IP_ATTR = "clientIp"

    const val SYSTEM_TRACE_ID = "SYSTEM"

    private const val REACTOR_TRACE_ID_KEY = "request.traceId"
    private const val REACTOR_USER_ID_KEY = "request.userId"
    private const val REACTOR_CLIENT_IP_KEY = "request.clientIp"

    fun writeToExchange(exchange: ServerWebExchange, context: RequestLogContext) {
        exchange.attributes[TRACE_ID_ATTR] = context.traceId
        context.userId?.let { exchange.attributes[USER_ID_ATTR] = it.toString() }
        context.clientIp?.let { exchange.attributes[CLIENT_IP_ATTR] = it }
    }

    fun normalizeTraceId(rawTraceId: String?): String {
        val normalized = rawTraceId?.trim()
        return if (normalized.isNullOrBlank()) SYSTEM_TRACE_ID else normalized
    }

    fun writeToReactor(context: RequestLogContext): (Context) -> Context = { reactorContext ->
        reactorContext
            .put(REACTOR_TRACE_ID_KEY, context.traceId)
            .let {
                if (context.userId != null) it.put(REACTOR_USER_ID_KEY, context.userId) else it
            }
            .let {
                if (context.clientIp != null) it.put(REACTOR_CLIENT_IP_KEY, context.clientIp) else it
            }
    }

    suspend fun current(): RequestLogContext? {
        val reactorContext = runCatching { coroutineContext[ReactorContext]?.context }.getOrNull() ?: return null
        return fromReactorContext(reactorContext)
    }

    fun fromContinuation(continuation: Continuation<*>): RequestLogContext? {
        val reactorContext = runCatching { continuation.context[ReactorContext]?.context }.getOrNull() ?: return null
        return fromReactorContext(reactorContext)
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

    private fun fromReactorContext(reactorContext: Context): RequestLogContext? {
        val traceId = reactorContext.getOrDefault<String?>(REACTOR_TRACE_ID_KEY, null) ?: return null
        val userId = reactorContext.getOrDefault<Long?>(REACTOR_USER_ID_KEY, null)
        val clientIp = reactorContext.getOrDefault<String?>(REACTOR_CLIENT_IP_KEY, null)
        return RequestLogContext(
            traceId = traceId,
            userId = userId,
            clientIp = clientIp
        )
    }
}

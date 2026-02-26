package server.global.logging

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.core.Ordered
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import server.security.TokenDecodeCacheAttributes
import server.shared.security.jwt.AuthPrincipal

@Component
class RequestBoundaryLogFilter : WebFilter, Ordered {
    private val logger = KotlinLogging.logger {}

    override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE + 1

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val traceId = RequestLogContextHolder.normalizeTraceId(
            exchange.request.headers.getFirst(RequestLogContextHolder.TRACE_ID_HEADER)
        )
        val userId = resolveUserId(exchange)
        val clientIp = resolveClientIp(exchange)
        val context = RequestLogContext(
            traceId = traceId,
            userId = userId,
            clientIp = clientIp
        )

        exchange.response.headers.set(RequestLogContextHolder.TRACE_ID_HEADER, traceId)
        RequestLogContextHolder.writeToExchange(exchange, context)

        val method = exchange.request.method.name()
        val path = exchange.request.path.value()
        val isActuatorRequest = path == "/api/actuator" || path.startsWith("/api/actuator/")
        val startedAt = System.nanoTime()

        return chain.filter(exchange)
            .contextWrite(RequestLogContextHolder.writeToReactor(context))
            .doFinally {
                if (isActuatorRequest) return@doFinally
                val status = exchange.response.statusCode?.value() ?: 200
                val latencyMs = (System.nanoTime() - startedAt) / 1_000_000
                logger.request.info(
                    traceId = traceId,
                    "method" to method,
                    "path" to path,
                    "status" to status,
                    "latencyMs" to latencyMs,
                    "userId" to (userId?.toString() ?: "none"),
                ) {
                    "요청 처리가 완료되었습니다"
                }
            }
    }

    private fun resolveUserId(exchange: ServerWebExchange): Long? {
        val principal = exchange.attributes[TokenDecodeCacheAttributes.AUTH_PRINCIPAL_ATTR] as? AuthPrincipal
        return principal?.memberId
    }

    private fun resolveClientIp(exchange: ServerWebExchange): String? {
        val xForwardedFor = exchange.request.headers.getFirst("X-Forwarded-For")
            ?.split(",")
            ?.firstOrNull()
            ?.trim()
        if (!xForwardedFor.isNullOrBlank()) return xForwardedFor

        val xRealIp = exchange.request.headers.getFirst("X-Real-IP")
        if (!xRealIp.isNullOrBlank()) return xRealIp

        return exchange.request.remoteAddress?.address?.hostAddress
    }
}

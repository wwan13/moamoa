package server.admin.global.logging

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import server.global.logging.RequestLogContext
import server.global.logging.RequestLogContextHolder
import server.global.logging.request

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
internal class AdminRequestBoundaryLogFilter : OncePerRequestFilter() {
    private val log = KotlinLogging.logger {}

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        return !path.startsWith("/admin")
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val traceId = RequestLogContextHolder.normalizeTraceId(
            request.getHeader(RequestLogContextHolder.TRACE_ID_HEADER)
        )
        val userId = request.getAttribute(RequestLogContextHolder.USER_ID_ATTR)?.toString()?.toLongOrNull()
        val clientIp = resolveClientIp(request)
        val context = RequestLogContext(
            traceId = traceId,
            userId = userId,
            clientIp = clientIp,
        )

        response.setHeader(RequestLogContextHolder.TRACE_ID_HEADER, traceId)
        request.setAttribute(RequestLogContextHolder.TRACE_ID_ATTR, context.traceId)
        context.userId?.let { request.setAttribute(RequestLogContextHolder.USER_ID_ATTR, it.toString()) }
        context.clientIp?.let { request.setAttribute(RequestLogContextHolder.CLIENT_IP_ATTR, it) }

        val startedAt = System.nanoTime()
        try {
            RequestLogContextHolder.withContext(context) {
                filterChain.doFilter(request, response)
            }
        } finally {
            val latencyMs = (System.nanoTime() - startedAt) / 1_000_000
            log.request.info(
                traceId = traceId,
                "method" to request.method,
                "path" to request.requestURI,
                "status" to response.status,
                "latencyMs" to latencyMs,
                "userId" to (userId?.toString() ?: "none"),
            ) {
                "요청 처리가 완료되었습니다"
            }
        }
    }

    private fun resolveClientIp(request: HttpServletRequest): String? {
        val xForwardedFor = request.getHeader("X-Forwarded-For")
            ?.split(",")
            ?.firstOrNull()
            ?.trim()
        if (!xForwardedFor.isNullOrBlank()) return xForwardedFor

        val xRealIp = request.getHeader("X-Real-IP")
        if (!xRealIp.isNullOrBlank()) return xRealIp

        return request.remoteAddr
    }
}

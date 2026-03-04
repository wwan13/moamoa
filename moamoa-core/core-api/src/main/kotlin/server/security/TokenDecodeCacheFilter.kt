package server.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import server.global.logging.RequestLogContextHolder
import server.shared.security.jwt.ExpiredTokenException
import server.shared.security.jwt.InvalidTokenException
import server.shared.security.jwt.TokenProvider

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class TokenDecodeCacheFilter(
    private val tokenProvider: TokenProvider,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val authorization = request.getHeader(HttpHeaders.AUTHORIZATION)
        if (authorization.isNullOrBlank() || !authorization.startsWith("Bearer ", ignoreCase = true)) {
            filterChain.doFilter(request, response)
            return
        }

        val accessToken = authorization.substringAfter(' ', "").trim()
        if (accessToken.isBlank()) {
            filterChain.doFilter(request, response)
            return
        }

        try {
            val principal = tokenProvider.decodeToken(accessToken)
            request.setAttribute(TokenDecodeCacheAttributes.AUTH_PRINCIPAL_ATTR, principal)
            request.setAttribute(RequestLogContextHolder.USER_ID_ATTR, principal.memberId.toString())
        } catch (e: InvalidTokenException) {
            request.setAttribute(TokenDecodeCacheAttributes.TOKEN_DECODE_ERROR_ATTR, e)
        } catch (e: ExpiredTokenException) {
            request.setAttribute(TokenDecodeCacheAttributes.TOKEN_DECODE_ERROR_ATTR, e)
        }

        filterChain.doFilter(request, response)
    }
}

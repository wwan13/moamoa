package server.core.global.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import server.global.logging.RequestLogContextHolder
import server.token.ExpiredTokenException
import server.token.InvalidTokenException
import server.token.TokenProvider

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
        val accessToken = request.resolveAccessToken()
        if (accessToken.isNullOrBlank()) {
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

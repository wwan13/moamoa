package server.core.global.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import server.core.feature.member.infra.BlackListSet
import server.global.logging.RequestLogContextHolder
import server.core.global.security.TokenDecodeCacheAttributes.BLACKLIST_ERROR_ATTR
import server.token.ExpiredTokenException
import server.token.InvalidTokenException
import server.token.TokenProvider

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class TokenDecodeCacheFilter(
    private val tokenProvider: TokenProvider,
    private val blackListSet: BlackListSet,
) : OncePerRequestFilter() {
    private val log = KotlinLogging.logger {}

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
            if (isBlacklisted(principal.memberId)) {
                request.setAttribute(RequestLogContextHolder.USER_ID_ATTR, principal.memberId.toString())
                request.setAttribute(BLACKLIST_ERROR_ATTR, ForbiddenException("차단된 사용자입니다"))
                filterChain.doFilter(request, response)
                return
            }
            request.setAttribute(TokenDecodeCacheAttributes.AUTH_PRINCIPAL_ATTR, principal)
            request.setAttribute(RequestLogContextHolder.USER_ID_ATTR, principal.memberId.toString())
        } catch (e: InvalidTokenException) {
            request.setAttribute(TokenDecodeCacheAttributes.TOKEN_DECODE_ERROR_ATTR, e)
        } catch (e: ExpiredTokenException) {
            request.setAttribute(TokenDecodeCacheAttributes.TOKEN_DECODE_ERROR_ATTR, e)
        }

        filterChain.doFilter(request, response)
    }

    private fun isBlacklisted(memberId: Long): Boolean =
        runCatching { blackListSet.contains(memberId) }
            .onFailure { log.warn(it) { "회원 블랙리스트 조회에 실패했습니다. memberId=$memberId" } }
            .getOrElse { false }
}

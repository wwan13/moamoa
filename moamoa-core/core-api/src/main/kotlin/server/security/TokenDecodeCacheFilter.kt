package server.security

import org.springframework.core.Ordered
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import server.shared.security.jwt.ExpiredTokenException
import server.shared.security.jwt.InvalidTokenException
import server.shared.security.jwt.TokenProvider

@Component
class TokenDecodeCacheFilter(
    private val tokenProvider: TokenProvider,
) : WebFilter, Ordered {

    override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val authorization = exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION)
            ?: return chain.filter(exchange)
        if (!authorization.startsWith("Bearer ", ignoreCase = true)) {
            return chain.filter(exchange)
        }

        val accessToken = authorization.substringAfter(' ', "").trim()
        if (accessToken.isBlank()) {
            return chain.filter(exchange)
        }

        try {
            val principal = tokenProvider.decodeToken(accessToken)
            exchange.attributes[TokenDecodeCacheAttributes.AUTH_PRINCIPAL_ATTR] = principal
        } catch (e: InvalidTokenException) {
            exchange.attributes[TokenDecodeCacheAttributes.TOKEN_DECODE_ERROR_ATTR] = e
        } catch (e: ExpiredTokenException) {
            exchange.attributes[TokenDecodeCacheAttributes.TOKEN_DECODE_ERROR_ATTR] = e
        }

        return chain.filter(exchange)
    }
}

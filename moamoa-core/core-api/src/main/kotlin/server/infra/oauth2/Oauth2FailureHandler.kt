package server.infra.oauth2

import org.springframework.http.HttpStatus
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.server.WebFilterExchange
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.net.URI

@Component
class Oauth2FailureHandler : ServerAuthenticationFailureHandler {
    override fun onAuthenticationFailure(
        webFilterExchange: WebFilterExchange,
        exception: AuthenticationException?
    ): Mono<Void> {
        val exchange = webFilterExchange.exchange
        val request = exchange.request

        val path = request.path.value()

        val registrationId = path
            .substringAfter("/login/oauth2/callback/")
            .substringBefore("?")
            .takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("registrationId 를 path 에서 찾을 수 없습니다")

        val redirectUri = URI.create("/oauth2/authorization/$registrationId")

        exchange.response.statusCode = HttpStatus.FOUND
        exchange.response.headers.location = redirectUri
        return exchange.response.setComplete()
    }
}
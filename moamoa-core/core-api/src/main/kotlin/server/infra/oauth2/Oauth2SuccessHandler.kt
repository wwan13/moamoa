package server.infra.oauth2

import kotlinx.coroutines.reactor.mono
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.security.web.server.WebFilterExchange
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import server.feature.auth.application.AuthService
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Component
class Oauth2SuccessHandler(
    private val authService: AuthService,
    private val environment: Environment
) : ServerAuthenticationSuccessHandler {

    override fun onAuthenticationSuccess(
        webFilterExchange: WebFilterExchange,
        authentication: Authentication
    ): Mono<Void> {
        return mono {
            val authenticatedUser = authentication.principal as Oauth2SocialUser

            when (authenticatedUser) {
                is Oauth2SocialUser.Authenticated -> {
                    val tokens = authService.issueTokens(authenticatedUser.memberId, authenticatedUser.role.toString())

                    redirectUrl(
                        "type" to "success",
                        "accessToken" to tokens.accessToken,
                        "refreshToken" to tokens.refreshToken,
                        "isNew" to authenticatedUser.isNew.toString()
                    )
                }
                is Oauth2SocialUser.EmailRequired -> {
                    redirectUrl(
                        "emailRequired" to "emailRequired",
                        "provider" to authenticatedUser.provider.name,
                        "providerKey" to authenticatedUser.providerKey
                    )
                }
                is Oauth2SocialUser.HasError -> {
                    redirectUrl(
                        "type" to "hasError",
                        "errorMessage" to authenticatedUser.message,
                    )
                }
            }
        }.flatMap { redirectUrl ->
            val response = webFilterExchange.exchange.response
            response.statusCode = HttpStatus.FOUND
            response.headers.location = URI.create(redirectUrl)
            response.setComplete()
        }
    }

    private fun redirectUrl(vararg param: Pair<String, String>): String {
        val queryParams = param.joinToString("&") { (k, v) ->
            "${enc(k)}=${enc(v)}"
        }
        val base = if (environment.activeProfiles.contains("prod")) {
            "https://moamoa.wowan.me/oauth2"
        } else {
            "http://localhost:5173/oauth2"
        }
        return "$base?$queryParams"
    }

    private fun enc(s: String) =
        URLEncoder.encode(s, StandardCharsets.UTF_8)
}
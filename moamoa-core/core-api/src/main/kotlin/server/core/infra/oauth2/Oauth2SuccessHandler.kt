package server.core.infra.oauth2

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component
import server.core.feature.auth.application.AuthService
import server.core.feature.auth.infra.RefreshTokenCache
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Component
class Oauth2SuccessHandler(
    private val authService: AuthService,
    private val refreshTokenCache: RefreshTokenCache,
    private val environment: Environment
) : AuthenticationSuccessHandler {

    private val refreshTokenExpires = 604_800_000L

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication,
    ) {
        val authenticatedUser = authentication.principal as Oauth2SocialUser

        val redirectUrl = when (authenticatedUser) {
            is Oauth2SocialUser.Authenticated -> {
                val tokens = authService.issueTokens(authenticatedUser.memberId, authenticatedUser.role.toString())
                refreshTokenCache.set(authenticatedUser.memberId, tokens.refreshToken, refreshTokenExpires)

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

        response.status = HttpStatus.FOUND.value()
        response.sendRedirect(redirectUrl)
    }

    private fun redirectUrl(vararg param: Pair<String, String>): String {
        val queryParams = param.joinToString("&") { (k, v) ->
            "${enc(k)}=${enc(v)}"
        }
        val base = if (environment.activeProfiles.contains("prod")) {
            "https://moamoa.dev/oauth2"
        } else {
            "http://localhost:5173/oauth2"
        }
        return "$base?$queryParams"
    }

    private fun enc(s: String) =
        URLEncoder.encode(s, StandardCharsets.UTF_8)
}

package server.core.feature.auth.api

import jakarta.validation.Valid
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import server.core.feature.auth.application.AuthService
import server.core.feature.auth.application.AuthTokens
import server.core.feature.auth.application.LoginCommand
import server.core.feature.auth.application.LoginSocialSessionCommand
import server.core.global.security.AuthCookieSupport
import server.core.global.security.appendAuthCookies
import server.core.global.security.expireAuthCookies
import server.core.global.security.Passport
import server.core.global.security.RequestPassport
import server.core.global.security.resolveRefreshToken
import server.core.global.security.UnauthorizedException
import server.core.global.web.ApiResponse

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/login")
    fun login(
        @RequestBody @Valid command: LoginCommand,
        response: HttpServletResponse,
    ): ApiResponse<AuthTokens> {
        val tokens = authService.login(command)
        response.appendAuthCookies(tokens.accessToken, tokens.refreshToken)

        return ApiResponse.of(tokens)
    }

    @PostMapping("/reissue")
    fun reissue(
        @RequestHeader(AuthCookieSupport.REFRESH_TOKEN_HEADER, required = false) refreshTokenHeader: String?,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): ApiResponse<AuthTokens> {
        val refreshToken = refreshTokenHeader
            ?.takeIf { it.isNotBlank() }
            ?: request.resolveRefreshToken()
            ?: throw UnauthorizedException()
        val tokens = authService.reissue(refreshToken)
        response.appendAuthCookies(tokens.accessToken, tokens.refreshToken)

        return ApiResponse.of(tokens)
    }

    @PostMapping("/logout")
    fun logout(
        @RequestPassport passport: Passport?,
        httpResponse: HttpServletResponse,
    ): ApiResponse<Unit> {
        passport?.also {
            authService.logout(it.memberId)
        }
        httpResponse.expireAuthCookies()

        return ApiResponse.of()
    }

    @PostMapping("/login/social")
    fun loginSocialSession(
        @RequestBody @Valid command: LoginSocialSessionCommand,
        response: HttpServletResponse,
    ): ApiResponse<AuthTokens> {
        val tokens = authService.loginSocialSession(command)
        response.appendAuthCookies(tokens.accessToken, tokens.refreshToken)

        return ApiResponse.of(tokens)
    }
}

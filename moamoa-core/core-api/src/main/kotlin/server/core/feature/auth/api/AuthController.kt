package server.core.feature.auth.api

import jakarta.validation.Valid
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import server.core.feature.auth.application.AuthService
import server.core.feature.auth.application.AuthTokens
import server.core.feature.auth.application.ConfirmEmailCommand
import server.core.feature.auth.application.ConfirmEmailResult
import server.core.feature.auth.application.EmailVerificationCommand
import server.core.feature.auth.application.EmailVerificationResult
import server.core.feature.auth.application.LoginCommand
import server.core.feature.auth.application.LoginSocialSessionCommand
import server.core.feature.auth.application.LogoutResult
import server.core.global.security.AuthCookieSupport
import server.core.global.security.appendAuthCookies
import server.core.global.security.expireAuthCookies
import server.core.global.security.Passport
import server.core.global.security.RequestPassport
import server.core.global.security.resolveRefreshToken
import server.core.global.security.UnauthorizedException

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/email-verification")
    fun emailVerification(
        @RequestBody @Valid command: EmailVerificationCommand
    ): ResponseEntity<EmailVerificationResult> {
        val response =  authService.emailVerification(command)

        return ResponseEntity.ok(response)
    }

    @PostMapping("/email-verification/confirm")
    fun emailVerificationConfirm(
        @RequestBody @Valid command: ConfirmEmailCommand
    ): ResponseEntity<ConfirmEmailResult> {
        val response = authService.confirmEmail(command)

        return ResponseEntity.ok(response)
    }

    @PostMapping("/login")
    fun login(
        @RequestBody @Valid command: LoginCommand,
        response: HttpServletResponse,
    ): ResponseEntity<AuthTokens> {
        val tokens = authService.login(command)
        response.appendAuthCookies(tokens.accessToken, tokens.refreshToken)

        return ResponseEntity.ok(tokens)
    }

    @PostMapping("/reissue")
    fun reissue(
        @RequestHeader(AuthCookieSupport.REFRESH_TOKEN_HEADER, required = false) refreshTokenHeader: String?,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): ResponseEntity<AuthTokens> {
        val refreshToken = refreshTokenHeader
            ?.takeIf { it.isNotBlank() }
            ?: request.resolveRefreshToken()
            ?: throw UnauthorizedException()
        val tokens = authService.reissue(refreshToken)
        response.appendAuthCookies(tokens.accessToken, tokens.refreshToken)

        return ResponseEntity.ok(tokens)
    }

    @PostMapping("/logout")
    fun logout(
        @RequestPassport passport: Passport,
        httpResponse: HttpServletResponse,
    ): ResponseEntity<LogoutResult> {
        val result = authService.logout(passport.memberId)
        httpResponse.expireAuthCookies()

        return ResponseEntity.ok(result)
    }

    @PostMapping("/login/social")
    fun loginSocialSession(
        @RequestBody @Valid command: LoginSocialSessionCommand,
        response: HttpServletResponse,
    ): ResponseEntity<AuthTokens> {
        val tokens = authService.loginSocialSession(command)
        response.appendAuthCookies(tokens.accessToken, tokens.refreshToken)

        return ResponseEntity.ok(tokens)
    }
}

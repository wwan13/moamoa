package server.feature.auth.api

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import server.feature.auth.application.AuthService
import server.feature.auth.application.AuthTokens
import server.feature.auth.application.ConfirmEmailCommand
import server.feature.auth.application.ConfirmEmailResult
import server.feature.auth.application.EmailVerificationCommand
import server.feature.auth.application.EmailVerificationResult
import server.feature.auth.application.LoginCommand
import server.feature.auth.application.LoginSocialSessionCommand
import server.feature.auth.application.LogoutResult
import server.security.Passport
import server.security.RequestPassport

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/email-verification")
    suspend fun emailVerification(
        @RequestBody @Valid command: EmailVerificationCommand
    ): ResponseEntity<EmailVerificationResult> {
        val response =  authService.emailVerification(command)

        return ResponseEntity.ok(response)
    }

    @PostMapping("/email-verification/confirm")
    suspend fun emailVerificationConfirm(
        @RequestBody @Valid command: ConfirmEmailCommand
    ): ResponseEntity<ConfirmEmailResult> {
        val response = authService.confirmEmail(command)

        return ResponseEntity.ok(response)
    }

    @PostMapping("/login")
    suspend fun login(
        @RequestBody @Valid command: LoginCommand
    ): ResponseEntity<AuthTokens> {
        val tokens = authService.login(command)

        return ResponseEntity.ok(tokens)
    }

    @PostMapping("/reissue")
    suspend fun reissue(
        @RequestHeader("X-Refresh-Token") refreshToken: String
    ): ResponseEntity<AuthTokens> {
        val tokens = authService.reissue(refreshToken)

        return ResponseEntity.ok(tokens)
    }

    @PostMapping("/logout")
    suspend fun logout(
        @RequestPassport passport: Passport
    ): ResponseEntity<LogoutResult> {
        val response = authService.logout(passport.memberId)

        return ResponseEntity.ok(response)
    }

    @PostMapping("/login/social")
    suspend fun loginSocialSession(
        @RequestBody @Valid command: LoginSocialSessionCommand
    ): ResponseEntity<AuthTokens> {
        val tokens = authService.loginSocialSession(command)

        return ResponseEntity.ok(tokens)
    }
}
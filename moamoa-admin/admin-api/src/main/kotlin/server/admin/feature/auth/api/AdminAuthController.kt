package server.admin.feature.auth.api

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import server.admin.feature.auth.application.AdminAuthService
import server.admin.feature.auth.application.AdminAuthTokens
import server.admin.feature.auth.application.AdminLoginCommand
import server.admin.feature.auth.application.AdminLogoutResult
import server.admin.global.security.*

@RestController
@RequestMapping("/api/admin/auth")
internal class AdminAuthController(
    private val authService: AdminAuthService
) {

    @PostMapping("/login")
    fun login(
        @RequestBody @Valid command: AdminLoginCommand,
        response: HttpServletResponse,
    ): ResponseEntity<AdminAuthTokens> {
        val tokens = authService.adminLogin(command)
        response.appendAdminAuthCookies(tokens.accessToken, tokens.refreshToken)
        return ResponseEntity.ok(tokens)
    }

    @PostMapping("/reissue")
    fun reissue(
        @RequestHeader(AdminAuthCookieSupport.REFRESH_TOKEN_HEADER, required = false) refreshTokenHeader: String?,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): ResponseEntity<AdminAuthTokens> {
        val refreshToken = refreshTokenHeader
            ?.takeIf { it.isNotBlank() }
            ?: request.resolveAdminRefreshToken()
            ?: throw AdminUnauthorizedException()
        val tokens = authService.adminReissue(refreshToken)
        response.appendAdminAuthCookies(tokens.accessToken, tokens.refreshToken)
        return ResponseEntity.ok(tokens)
    }

    @PostMapping("/logout")
    fun logout(
        @RequestAdminPassport passport: AdminPassport,
        response: HttpServletResponse,
    ): ResponseEntity<AdminLogoutResult> {
        val result = authService.logout(passport.memberId)
        response.expireAdminAuthCookies()
        return ResponseEntity.ok(result)
    }
}

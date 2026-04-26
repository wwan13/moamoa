package server.admin.feature.auth.api

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*
import server.admin.feature.auth.application.AdminAuthService
import server.admin.feature.auth.application.AdminAuthTokens
import server.admin.feature.auth.application.AdminLoginCommand
import server.admin.global.security.*
import server.admin.global.web.AdminApiResponse

@RestController
@RequestMapping("/api/admin/auth")
internal class AdminAuthController(
    private val authService: AdminAuthService
) {

    @PostMapping("/login")
    fun login(
        @RequestBody @Valid command: AdminLoginCommand,
        response: HttpServletResponse,
    ): AdminApiResponse<AdminAuthTokens> {
        val tokens = authService.adminLogin(command)
        response.appendAdminAuthCookies(tokens.accessToken, tokens.refreshToken)
        return AdminApiResponse.of(tokens)
    }

    @PostMapping("/reissue")
    fun reissue(
        @RequestHeader(AdminAuthCookieSupport.REFRESH_TOKEN_HEADER, required = false) refreshTokenHeader: String?,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): AdminApiResponse<AdminAuthTokens> {
        val refreshToken = refreshTokenHeader
            ?.takeIf { it.isNotBlank() }
            ?: request.resolveAdminRefreshToken()
            ?: throw AdminUnauthorizedException()
        val tokens = authService.adminReissue(refreshToken)
        response.appendAdminAuthCookies(tokens.accessToken, tokens.refreshToken)
        return AdminApiResponse.of(tokens)
    }

    @PostMapping("/logout")
    fun logout(
        @RequestAdminPassport passport: AdminPassport,
        response: HttpServletResponse,
    ): AdminApiResponse<Unit> {
        authService.logout(passport.memberId)
        response.expireAdminAuthCookies()
        return AdminApiResponse.of()
    }
}

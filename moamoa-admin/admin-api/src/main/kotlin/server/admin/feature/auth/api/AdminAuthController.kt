package server.admin.feature.auth.api

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import server.admin.feature.auth.application.AdminAuthService
import server.admin.feature.auth.application.AdminAuthTokens
import server.admin.feature.auth.application.AdminLoginCommand
import server.admin.feature.auth.application.AdminLogoutResult
import server.admin.global.security.AdminPassport
import server.admin.global.security.RequestAdminPassport

@RestController
@RequestMapping("/api/admin/auth")
internal class AdminAuthController(
    private val authService: AdminAuthService
) {

    @PostMapping("/login")
    fun login(
        @RequestBody @Valid command: AdminLoginCommand
    ): ResponseEntity<AdminAuthTokens> {
        val tokens = authService.adminLogin(command)
        return ResponseEntity.ok(tokens)
    }

    @PostMapping("/reissue")
    fun reissue(
        @RequestHeader("X-Refresh-Token") refreshToken: String
    ): ResponseEntity<AdminAuthTokens> {
        val tokens = authService.adminReissue(refreshToken)
        return ResponseEntity.ok(tokens)
    }

    @PostMapping("/logout")
    fun logout(
        @RequestAdminPassport passport: AdminPassport
    ): ResponseEntity<AdminLogoutResult> {
        val response = authService.logout(passport.memberId)
        return ResponseEntity.ok(response)
    }
}

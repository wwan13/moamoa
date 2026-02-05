package server.admin.feature.auth.application

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class AdminLoginCommand(
    @field:NotBlank
    @field:Email
    val email: String,

    @field:NotBlank
    @field:Size(min = 8, max = 32)
    @field:Pattern(regexp = "^(?=.*[^A-Za-z0-9])[A-Za-z0-9[^A-Za-z0-9]]{8,32}$")
    val password: String
)

data class AdminAuthTokens(
    val accessToken: String,
    val refreshToken: String
)

data class AdminLogoutResult(
    val success: Boolean
)

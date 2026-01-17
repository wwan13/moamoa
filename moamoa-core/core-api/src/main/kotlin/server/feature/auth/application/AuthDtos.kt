package server.feature.auth.application

import jakarta.validation.constraints.*

data class EmailVerificationCommand(
    @field:NotBlank
    @field:Email
    val email: String
)

data class EmailVerificationResult(
    val success: Boolean
)

data class ConfirmEmailCommand(
    @field:NotBlank
    @field:Email
    val email: String,

    @field:NotBlank
    @field:Pattern(regexp = "^[0-9]{6}$")
    val code: String
)

data class ConfirmEmailResult(
    val isConfirmed: Boolean
)

data class LoginCommand(
    @field:NotBlank
    @field:Email
    val email: String,

    @field:NotBlank
    @field:Size(min = 8, max = 24)
    @field:Pattern(regexp = "^(?=.*[^A-Za-z0-9])[A-Za-z0-9[^A-Za-z0-9]]{8,32}$")
    val password: String
)

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String
)

data class LogoutResult(
    val success: Boolean
)

data class LoginSocialSessionCommand(
    @field:NotBlank
    val token: String,

    @field:NotNull
    val memberId: Long
)
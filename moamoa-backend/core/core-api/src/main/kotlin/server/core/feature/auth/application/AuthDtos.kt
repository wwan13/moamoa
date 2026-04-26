package server.core.feature.auth.application

import jakarta.validation.constraints.*

data class LoginCommand(
    @field:NotBlank
    @field:Email
    val email: String,

    @field:NotBlank
    @field:Size(min = 8, max = 32)
    @field:Pattern(regexp = "^(?=.*[^A-Za-z0-9])[A-Za-z0-9[^A-Za-z0-9]]{8,32}$")
    val password: String
)

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String
)

data class LoginSocialSessionCommand(
    @field:NotBlank
    val token: String,

    @field:NotNull
    val memberId: Long
)

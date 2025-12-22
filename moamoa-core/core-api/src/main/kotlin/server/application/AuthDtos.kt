package server.application

data class EmailVerificationCommand(
    val email: String
)

data class EmailVerificationResult(
    val success: Boolean
)

data class ConfirmEmailCommand(
    val email: String,
    val code: String
)

data class ConfirmEmailResult(
    val isConfirmed: Boolean
)

data class LoginCommand(
    val email: String,
    val password: String
)

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String
)
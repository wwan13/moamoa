package server.shared.security.jwt

interface TokenProvider {
    fun encodeToken(
        principal: AuthPrincipal,
        ttl: Long
    ): String

    fun decodeToken(token: String): AuthPrincipal
}

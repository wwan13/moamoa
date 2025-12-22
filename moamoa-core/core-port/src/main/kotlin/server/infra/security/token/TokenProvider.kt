package server.infra.security.token

interface TokenProvider {
    fun encodeToken(
        principal: AuthPrincipal,
        ttl: Long
    ): String

    fun decodeToken(token: String): AuthPrincipal
}
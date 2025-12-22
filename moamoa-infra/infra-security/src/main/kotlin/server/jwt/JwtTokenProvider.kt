package server.jwt

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import server.config.SecurityProperties
import server.infra.security.token.AuthPrincipal
import server.infra.security.token.TokenProvider
import server.infra.security.token.TokenType
import java.util.*

@Component
class JwtTokenProvider(
    props: SecurityProperties
) : TokenProvider {

    private val key = Keys.hmacShaKeyFor(props.secretKey.toByteArray())

    override fun encodeToken(
        principal: AuthPrincipal,
        ttl: Long
    ): String {
        val now = Date()
        val expiry = Date(now.time + ttl)

        return Jwts.builder()
            .subject(principal.memberId.toString())
            .claim("type", principal.type.name)
            .apply {
                if (principal.type == TokenType.ACCESS) {
                    claim("role", principal.role)
                }
            }
            .issuedAt(now)
            .expiration(expiry)
            .signWith(key)
            .compact()
    }

    @Suppress("UNCHECKED_CAST")
    override fun decodeToken(token: String): AuthPrincipal {
        val payload = try {
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .payload
        } catch (e: ExpiredJwtException) {
            throw IllegalArgumentException("TOKEN_EXPIRED")
        } catch (e: Exception) {
            throw IllegalArgumentException("INVALID_TOKEN")
        }

        val memberId = payload.subject.toLongOrNull()
            ?: throw IllegalArgumentException("INVALID_TOKEN")
        val type = payload.get("type", String::class.java)
            ?: throw IllegalArgumentException("INVALID_TOKEN")
        val role: String? = payload.get("role", String::class.java)

        return AuthPrincipal(
            memberId = memberId,
            role = role,
            type = TokenType.valueOf(type),
        )
    }
}
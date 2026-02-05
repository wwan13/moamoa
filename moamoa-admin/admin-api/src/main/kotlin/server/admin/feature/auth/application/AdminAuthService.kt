package server.admin.feature.auth.application

import org.springframework.stereotype.Service
import server.admin.feature.member.domain.AdminMemberRepository
import server.admin.infra.cache.AdminRefreshTokenCache
import server.admin.security.AdminForbiddenException
import server.admin.security.AdminUnauthorizedException
import server.jwt.AuthPrincipal
import server.jwt.TokenProvider
import server.jwt.TokenType
import server.password.PasswordEncoder

@Service
internal class AdminAuthService(
    private val memberRepository: AdminMemberRepository,
    private val passwordEncoder: PasswordEncoder,
    private val tokenProvider: TokenProvider,
    private val refreshTokenCache: AdminRefreshTokenCache
) {

    private val accessTokenExpires = 3_600_000L
    private val refreshTokenExpires = 604_800_000L

    suspend fun adminLogin(command: AdminLoginCommand): AdminAuthTokens {
        val member = memberRepository.findByEmail(command.email)
            ?: throw IllegalArgumentException("존재하지 않는 사용자 입니다.")

        if (!member.isAdmin) {
            throw AdminForbiddenException()
        }

        if (!passwordEncoder.matches(command.password, member.password)) {
            throw IllegalArgumentException("비밀번호가 일치하지 않습니다.")
        }

        val tokens = issueTokens(member.id, member.role.name)
        refreshTokenCache.set(member.id, tokens.refreshToken, refreshTokenExpires)

        return tokens
    }

    suspend fun adminReissue(refreshToken: String): AdminAuthTokens {
        val principal = tokenProvider.decodeToken(refreshToken)

        if (principal.type != TokenType.REFRESH) {
            throw AdminUnauthorizedException()
        }
        val savedToken = refreshTokenCache.get(principal.memberId)
            ?: throw AdminUnauthorizedException()
        if (refreshToken != savedToken) {
            throw AdminUnauthorizedException()
        }

        val member = memberRepository.findById(principal.memberId)
            ?: throw AdminUnauthorizedException()
        if (!member.isAdmin) {
            throw AdminForbiddenException()
        }

        val tokens = issueTokens(member.id, member.role.name)
        refreshTokenCache.set(member.id, tokens.refreshToken, refreshTokenExpires)

        return tokens
    }

    fun issueTokens(memberId: Long, role: String): AdminAuthTokens {
        val accessTokenPrincipal = AuthPrincipal.accessToken(memberId, role)
        val accessToken = tokenProvider.encodeToken(accessTokenPrincipal, accessTokenExpires)

        val refreshTokenPrincipal = AuthPrincipal.refreshToken(memberId)
        val refreshToken = tokenProvider.encodeToken(refreshTokenPrincipal, refreshTokenExpires)

        return AdminAuthTokens(accessToken, refreshToken)
    }

    suspend fun logout(memberId: Long): AdminLogoutResult {
        refreshTokenCache.evict(memberId)

        return AdminLogoutResult(true)
    }
}
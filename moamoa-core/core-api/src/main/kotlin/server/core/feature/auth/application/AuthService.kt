package server.core.feature.auth.application

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import server.core.feature.auth.infra.RefreshTokenCache
import server.core.feature.auth.infra.SocialMemberSessionCache
import server.core.feature.member.domain.MemberRepository
import server.core.global.security.UnauthorizedException
import server.password.PasswordEncoder
import server.token.AuthPrincipal
import server.token.TokenProvider
import server.token.TokenType

@Service
@Transactional
class AuthService(
    private val memberRepository: MemberRepository,
    private val tokenProvider: TokenProvider,
    private val passwordEncoder: PasswordEncoder,
    private val refreshTokenCache: RefreshTokenCache,
    private val socialMemberSessionCache: SocialMemberSessionCache
) {
    private val accessTokenExpires = 3_600_000L
    private val refreshTokenExpires = 604_800_000L

    @Transactional(readOnly = true)
    fun login(command: LoginCommand): AuthTokens {
        val member = memberRepository.findByEmail(command.email)
            ?: throw IllegalArgumentException("존재하지 않는 사용자 입니다.")

        if (!passwordEncoder.matches(command.password, member.password)) {
            throw IllegalArgumentException("비밀번호가 일치하지 않습니다.")
        }

        val tokens = issueTokens(member.id, member.role.name)
        refreshTokenCache.set(member.id, tokens.refreshToken, refreshTokenExpires)

        return tokens
    }

    @Transactional(readOnly = true)
    fun reissue(refreshToken: String): AuthTokens {
        val principal = tokenProvider.decodeToken(refreshToken)

        if (principal.type != TokenType.REFRESH) {
            throw UnauthorizedException()
        }
        val savedToken = refreshTokenCache.get(principal.memberId)
            ?: throw UnauthorizedException()
        if (refreshToken != savedToken) {
            throw UnauthorizedException()
        }

        val member = memberRepository.findByIdOrNull(principal.memberId)
            ?: throw UnauthorizedException()
        val tokens = issueTokens(member.id, member.role.name)
        refreshTokenCache.set(member.id, tokens.refreshToken, refreshTokenExpires)

        return tokens
    }

    @Transactional(readOnly = true)
    fun issueTokens(memberId: Long, role: String): AuthTokens {
        val accessTokenPrincipal = AuthPrincipal.accessToken(memberId, role)
        val accessToken = tokenProvider.encodeToken(accessTokenPrincipal, accessTokenExpires)

        val refreshTokenPrincipal = AuthPrincipal.refreshToken(memberId)
        val refreshToken = tokenProvider.encodeToken(refreshTokenPrincipal, refreshTokenExpires)

        return AuthTokens(accessToken, refreshToken)
    }

    @Transactional(readOnly = true)
    fun logout(memberId: Long) {
        refreshTokenCache.evict(memberId)
    }

    @Transactional(readOnly = true)
    fun loginSocialSession(command: LoginSocialSessionCommand): AuthTokens {
        val memberId = socialMemberSessionCache.get(command.token)
            ?: throw UnauthorizedException()

        if (memberId != command.memberId) {
            throw UnauthorizedException()
        }

        val member = memberRepository.findByIdOrNull(memberId)
            ?: throw UnauthorizedException()

        return issueTokens(member.id, member.role.name)
    }
}

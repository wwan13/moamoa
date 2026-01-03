package server.application

import org.springframework.stereotype.Service
import server.MailContent
import server.MailSender
import server.infra.cache.EmailVerificationCache
import server.infra.cache.RefreshTokenCache
import server.domain.member.MemberRepository
import server.jwt.AuthPrincipal
import server.jwt.TokenProvider
import server.jwt.TokenType
import server.password.PasswordEncoder
import server.security.UnauthorizedException
import server.template.mail.MailTemplate
import server.template.mail.toTemplateArgs
import java.security.SecureRandom

@Service
class AuthService(
    private val memberRepository: MemberRepository,
    private val emailVerificationCache: EmailVerificationCache,
    private val mailSender: MailSender,
    private val tokenProvider: TokenProvider,
    private val passwordEncoder: PasswordEncoder,
    private val refreshTokenCache: RefreshTokenCache,
) {

    private val accessTokenExpires = 3_600_000L
    private val refreshTokenExpires = 604_800_000L

    suspend fun emailVerification(command: EmailVerificationCommand): EmailVerificationResult {
        if (memberRepository.existsByEmail(command.email)) {
            throw IllegalArgumentException("이미 존재하는 이메일 입니다.")
        }

        val verificationCode = generateVerificationCode()

        val mailTemplate = MailTemplate.EmailVerification(
            homeUrl = "http://localhost:8080",
            verificationCode = verificationCode
        )
        val mailContent = MailContent.Template(
            to = command.email,
            subject = "이메일 인증 코드를 확인해주세요.",
            path = mailTemplate.path,
            args = mailTemplate.toTemplateArgs(),
        )

        mailSender.send(mailContent)
        emailVerificationCache.setVerificationCode(command.email, verificationCode)

        return EmailVerificationResult(true)
    }

    private fun generateVerificationCode() = SecureRandom()
        .nextInt(1_000_000)
        .toString()
        .padStart(6, '0')

    suspend fun confirmEmail(command: ConfirmEmailCommand): ConfirmEmailResult {
        val registered = emailVerificationCache.getVerificationCode(command.email)
            ?: throw IllegalArgumentException("인증번호를 먼저 전송해 주세요.")

        if (registered != command.code) {
            throw IllegalArgumentException("인증번호가 올바르지 않습니다.")
        }

        emailVerificationCache.setVerified(command.email)

        return ConfirmEmailResult(true)
    }

    suspend fun login(command: LoginCommand): AuthTokens {
        val member = memberRepository.findByEmail(command.email)
            ?: throw IllegalArgumentException("존재하지 않는 사용자 입니다.")

        if (!passwordEncoder.matches(command.password, member.password)) {
            throw IllegalArgumentException("비밀번호가 일치하지 않습니다.")
        }

        val tokens = issueTokens(member.id, member.role.name)
        refreshTokenCache.set(member.id, tokens.refreshToken, refreshTokenExpires)

        return tokens
    }

    suspend fun reissue(refreshToken: String): AuthTokens {
        val principal = tokenProvider.decodeToken(refreshToken)

        if (principal.type != TokenType.REFRESH) {
            throw UnauthorizedException()
        }
        val savedToken = refreshTokenCache.get(principal.memberId)
            ?: throw UnauthorizedException()
        if (refreshToken != savedToken) {
            throw UnauthorizedException()
        }

        val member = memberRepository.findById(principal.memberId)
            ?: throw UnauthorizedException()
        val tokens = issueTokens(member.id, member.role.name)
        refreshTokenCache.set(member.id, tokens.refreshToken, refreshTokenExpires)

        return tokens
    }

    private fun issueTokens(memberId: Long, role: String): AuthTokens {
        val accessTokenPrincipal = AuthPrincipal.accessToken(memberId, role)
        val accessToken = tokenProvider.encodeToken(accessTokenPrincipal, accessTokenExpires)

        val refreshTokenPrincipal = AuthPrincipal.refreshToken(memberId)
        val refreshToken = tokenProvider.encodeToken(refreshTokenPrincipal, refreshTokenExpires)

        return AuthTokens(accessToken, refreshToken)
    }

    suspend fun logout(memberId: Long): LogoutResult {
        refreshTokenCache.evict(memberId)

        return LogoutResult(true)
    }
}

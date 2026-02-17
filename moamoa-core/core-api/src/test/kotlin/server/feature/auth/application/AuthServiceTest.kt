package server.feature.auth.application

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import server.shared.mail.MailSender
import server.feature.member.command.domain.MemberRepository
import server.fixture.createMember
import server.infra.cache.EmailVerificationCache
import server.infra.cache.RefreshTokenCache
import server.infra.cache.SocialMemberSessionCache
import server.shared.security.jwt.AuthPrincipal
import server.shared.security.jwt.TokenProvider
import server.shared.security.jwt.TokenType
import server.shared.security.password.PasswordEncoder
import server.security.UnauthorizedException
import test.UnitTest

class AuthServiceTest : UnitTest() {
    @Test
    fun `이미 존재하는 이메일이면 이메일 인증 요청에서 예외가 발생한다`() = runTest {
        val memberRepository = mockk<MemberRepository>()
        val emailVerificationCache = mockk<EmailVerificationCache>()
        val mailSender = mockk<MailSender>(relaxed = true)
        val tokenProvider = mockk<TokenProvider>()
        val passwordEncoder = mockk<PasswordEncoder>()
        val refreshTokenCache = mockk<RefreshTokenCache>()
        val socialMemberSessionCache = mockk<SocialMemberSessionCache>()
        val service = AuthService(
            memberRepository,
            emailVerificationCache,
            mailSender,
            tokenProvider,
            passwordEncoder,
            refreshTokenCache,
            socialMemberSessionCache
        )

        val command = EmailVerificationCommand(email = "exist@example.com")

        coEvery { memberRepository.existsByEmail(command.email) } returns true

        val exception = shouldThrow<IllegalArgumentException> {
            service.emailVerification(command)
        }

        exception.message shouldBe "이미 존재하는 이메일 입니다."
        coVerify(exactly = 0) { emailVerificationCache.setVerificationCode(any(), any()) }
    }

    @Test
    fun `이메일 인증 요청 시 메일을 전송하고 인증코드를 저장한다`() = runTest {
        val memberRepository = mockk<MemberRepository>()
        val emailVerificationCache = mockk<EmailVerificationCache>()
        val mailSender = mockk<MailSender>(relaxed = true)
        val tokenProvider = mockk<TokenProvider>()
        val passwordEncoder = mockk<PasswordEncoder>()
        val refreshTokenCache = mockk<RefreshTokenCache>()
        val socialMemberSessionCache = mockk<SocialMemberSessionCache>()
        val service = AuthService(
            memberRepository,
            emailVerificationCache,
            mailSender,
            tokenProvider,
            passwordEncoder,
            refreshTokenCache,
            socialMemberSessionCache
        )

        val command = EmailVerificationCommand(email = "new@example.com")
        val codeSlot = slot<String>()

        coEvery { memberRepository.existsByEmail(command.email) } returns false
        coEvery { emailVerificationCache.setVerificationCode(command.email, capture(codeSlot)) } returns Unit

        val result = service.emailVerification(command)

        result.success shouldBe true
        Regex("^\\d{6}$").matches(codeSlot.captured) shouldBe true
    }

    @Test
    fun `이메일 인증 확인 시 인증번호가 없으면 예외가 발생한다`() = runTest {
        val memberRepository = mockk<MemberRepository>()
        val emailVerificationCache = mockk<EmailVerificationCache>()
        val mailSender = mockk<MailSender>(relaxed = true)
        val tokenProvider = mockk<TokenProvider>()
        val passwordEncoder = mockk<PasswordEncoder>()
        val refreshTokenCache = mockk<RefreshTokenCache>()
        val socialMemberSessionCache = mockk<SocialMemberSessionCache>()
        val service = AuthService(
            memberRepository,
            emailVerificationCache,
            mailSender,
            tokenProvider,
            passwordEncoder,
            refreshTokenCache,
            socialMemberSessionCache
        )

        val command = ConfirmEmailCommand(email = "new@example.com", code = "123456")

        coEvery { emailVerificationCache.getVerificationCode(command.email) } returns null

        val exception = shouldThrow<IllegalArgumentException> {
            service.confirmEmail(command)
        }

        exception.message shouldBe "인증번호를 먼저 전송해 주세요."
        coVerify(exactly = 0) { emailVerificationCache.setVerified(any()) }
    }

    @Test
    fun `이메일 인증 확인 시 인증번호가 다르면 예외가 발생한다`() = runTest {
        val memberRepository = mockk<MemberRepository>()
        val emailVerificationCache = mockk<EmailVerificationCache>()
        val mailSender = mockk<MailSender>(relaxed = true)
        val tokenProvider = mockk<TokenProvider>()
        val passwordEncoder = mockk<PasswordEncoder>()
        val refreshTokenCache = mockk<RefreshTokenCache>()
        val socialMemberSessionCache = mockk<SocialMemberSessionCache>()
        val service = AuthService(
            memberRepository,
            emailVerificationCache,
            mailSender,
            tokenProvider,
            passwordEncoder,
            refreshTokenCache,
            socialMemberSessionCache
        )

        val command = ConfirmEmailCommand(email = "new@example.com", code = "123456")

        coEvery { emailVerificationCache.getVerificationCode(command.email) } returns "654321"

        val exception = shouldThrow<IllegalArgumentException> {
            service.confirmEmail(command)
        }

        exception.message shouldBe "인증번호가 올바르지 않습니다."
        coVerify(exactly = 0) { emailVerificationCache.setVerified(any()) }
    }

    @Test
    fun `이메일 인증 확인 시 인증번호가 일치하면 verified를 저장한다`() = runTest {
        val memberRepository = mockk<MemberRepository>()
        val emailVerificationCache = mockk<EmailVerificationCache>()
        val mailSender = mockk<MailSender>(relaxed = true)
        val tokenProvider = mockk<TokenProvider>()
        val passwordEncoder = mockk<PasswordEncoder>()
        val refreshTokenCache = mockk<RefreshTokenCache>()
        val socialMemberSessionCache = mockk<SocialMemberSessionCache>()
        val service = AuthService(
            memberRepository,
            emailVerificationCache,
            mailSender,
            tokenProvider,
            passwordEncoder,
            refreshTokenCache,
            socialMemberSessionCache
        )

        val command = ConfirmEmailCommand(email = "new@example.com", code = "123456")

        coEvery { emailVerificationCache.getVerificationCode(command.email) } returns "123456"
        coEvery { emailVerificationCache.setVerified(command.email) } returns Unit

        val result = service.confirmEmail(command)

        result.isConfirmed shouldBe true
        coVerify(exactly = 1) { emailVerificationCache.setVerified(command.email) }
    }

    @Test
    fun `로그인 시 존재하지 않는 사용자면 예외가 발생한다`() = runTest {
        val memberRepository = mockk<MemberRepository>()
        val emailVerificationCache = mockk<EmailVerificationCache>()
        val mailSender = mockk<MailSender>(relaxed = true)
        val tokenProvider = mockk<TokenProvider>()
        val passwordEncoder = mockk<PasswordEncoder>()
        val refreshTokenCache = mockk<RefreshTokenCache>()
        val socialMemberSessionCache = mockk<SocialMemberSessionCache>()
        val service = AuthService(
            memberRepository,
            emailVerificationCache,
            mailSender,
            tokenProvider,
            passwordEncoder,
            refreshTokenCache,
            socialMemberSessionCache
        )

        val command = LoginCommand(email = "none@example.com", password = "password!!")

        coEvery { memberRepository.findByEmail(command.email) } returns null

        val exception = shouldThrow<IllegalArgumentException> {
            service.login(command)
        }

        exception.message shouldBe "존재하지 않는 사용자 입니다."
        verify(exactly = 0) { passwordEncoder.matches(any(), any()) }
    }

    @Test
    fun `로그인 시 비밀번호가 다르면 예외가 발생한다`() = runTest {
        val memberRepository = mockk<MemberRepository>()
        val emailVerificationCache = mockk<EmailVerificationCache>()
        val mailSender = mockk<MailSender>(relaxed = true)
        val tokenProvider = mockk<TokenProvider>()
        val passwordEncoder = mockk<PasswordEncoder>()
        val refreshTokenCache = mockk<RefreshTokenCache>()
        val socialMemberSessionCache = mockk<SocialMemberSessionCache>()
        val service = AuthService(
            memberRepository,
            emailVerificationCache,
            mailSender,
            tokenProvider,
            passwordEncoder,
            refreshTokenCache,
            socialMemberSessionCache
        )

        val command = LoginCommand(email = "user@example.com", password = "password!!")
        val member = createMember(id = 1L, email = command.email, password = "encoded")

        coEvery { memberRepository.findByEmail(command.email) } returns member
        every { passwordEncoder.matches(command.password, member.password) } returns false

        val exception = shouldThrow<IllegalArgumentException> {
            service.login(command)
        }

        exception.message shouldBe "비밀번호가 일치하지 않습니다."
        verify(exactly = 0) { tokenProvider.encodeToken(any(), any()) }
        coVerify(exactly = 0) { refreshTokenCache.set(any(), any(), any()) }
    }

    @Test
    fun `로그인 시 토큰을 발급하고 리프레시 토큰을 저장한다`() = runTest {
        val memberRepository = mockk<MemberRepository>()
        val emailVerificationCache = mockk<EmailVerificationCache>()
        val mailSender = mockk<MailSender>(relaxed = true)
        val tokenProvider = mockk<TokenProvider>()
        val passwordEncoder = mockk<PasswordEncoder>()
        val refreshTokenCache = mockk<RefreshTokenCache>()
        val socialMemberSessionCache = mockk<SocialMemberSessionCache>()
        val service = AuthService(
            memberRepository,
            emailVerificationCache,
            mailSender,
            tokenProvider,
            passwordEncoder,
            refreshTokenCache,
            socialMemberSessionCache
        )

        val command = LoginCommand(email = "user@example.com", password = "password!!")
        val member = createMember(id = 1L, email = command.email, password = "encoded")

        coEvery { memberRepository.findByEmail(command.email) } returns member
        every { passwordEncoder.matches(command.password, member.password) } returns true
        every { tokenProvider.encodeToken(any(), any()) } returnsMany listOf("access-token", "refresh-token")
        coEvery { refreshTokenCache.set(member.id, "refresh-token", 604_800_000L) } returns Unit

        val result = service.login(command)

        result.accessToken shouldBe "access-token"
        result.refreshToken shouldBe "refresh-token"
        verify(exactly = 1) {
            tokenProvider.encodeToken(
                match {
                    it.type == TokenType.ACCESS &&
                        it.memberId == member.id &&
                        it.role == member.role.name
                },
                3_600_000L
            )
        }
        verify(exactly = 1) {
            tokenProvider.encodeToken(
                match {
                    it.type == TokenType.REFRESH &&
                        it.memberId == member.id
                },
                604_800_000L
            )
        }
        coVerify(exactly = 1) { refreshTokenCache.set(member.id, "refresh-token", 604_800_000L) }
    }

    @Test
    fun `리프레시 토큰 재발급 시 토큰 타입이 다르면 예외가 발생한다`() = runTest {
        val memberRepository = mockk<MemberRepository>()
        val emailVerificationCache = mockk<EmailVerificationCache>()
        val mailSender = mockk<MailSender>(relaxed = true)
        val tokenProvider = mockk<TokenProvider>()
        val passwordEncoder = mockk<PasswordEncoder>()
        val refreshTokenCache = mockk<RefreshTokenCache>()
        val socialMemberSessionCache = mockk<SocialMemberSessionCache>()
        val service = AuthService(
            memberRepository,
            emailVerificationCache,
            mailSender,
            tokenProvider,
            passwordEncoder,
            refreshTokenCache,
            socialMemberSessionCache
        )

        val refreshToken = "refresh-token"

        every { tokenProvider.decodeToken(refreshToken) } returns AuthPrincipal.accessToken(1L, "USER")

        shouldThrow<UnauthorizedException> {
            service.reissue(refreshToken)
        }
    }

    @Test
    fun `리프레시 토큰 재발급 시 저장된 토큰이 없으면 예외가 발생한다`() = runTest {
        val memberRepository = mockk<MemberRepository>()
        val emailVerificationCache = mockk<EmailVerificationCache>()
        val mailSender = mockk<MailSender>(relaxed = true)
        val tokenProvider = mockk<TokenProvider>()
        val passwordEncoder = mockk<PasswordEncoder>()
        val refreshTokenCache = mockk<RefreshTokenCache>()
        val socialMemberSessionCache = mockk<SocialMemberSessionCache>()
        val service = AuthService(
            memberRepository,
            emailVerificationCache,
            mailSender,
            tokenProvider,
            passwordEncoder,
            refreshTokenCache,
            socialMemberSessionCache
        )

        val refreshToken = "refresh-token"
        val principal = AuthPrincipal.refreshToken(1L)

        every { tokenProvider.decodeToken(refreshToken) } returns principal
        coEvery { refreshTokenCache.get(principal.memberId) } returns null

        shouldThrow<UnauthorizedException> {
            service.reissue(refreshToken)
        }
    }

    @Test
    fun `리프레시 토큰 재발급 시 토큰이 일치하지 않으면 예외가 발생한다`() = runTest {
        val memberRepository = mockk<MemberRepository>()
        val emailVerificationCache = mockk<EmailVerificationCache>()
        val mailSender = mockk<MailSender>(relaxed = true)
        val tokenProvider = mockk<TokenProvider>()
        val passwordEncoder = mockk<PasswordEncoder>()
        val refreshTokenCache = mockk<RefreshTokenCache>()
        val socialMemberSessionCache = mockk<SocialMemberSessionCache>()
        val service = AuthService(
            memberRepository,
            emailVerificationCache,
            mailSender,
            tokenProvider,
            passwordEncoder,
            refreshTokenCache,
            socialMemberSessionCache
        )

        val refreshToken = "refresh-token"
        val principal = AuthPrincipal.refreshToken(1L)

        every { tokenProvider.decodeToken(refreshToken) } returns principal
        coEvery { refreshTokenCache.get(principal.memberId) } returns "other-token"

        shouldThrow<UnauthorizedException> {
            service.reissue(refreshToken)
        }
    }

    @Test
    fun `리프레시 토큰 재발급 시 사용자가 없으면 예외가 발생한다`() = runTest {
        val memberRepository = mockk<MemberRepository>()
        val emailVerificationCache = mockk<EmailVerificationCache>()
        val mailSender = mockk<MailSender>(relaxed = true)
        val tokenProvider = mockk<TokenProvider>()
        val passwordEncoder = mockk<PasswordEncoder>()
        val refreshTokenCache = mockk<RefreshTokenCache>()
        val socialMemberSessionCache = mockk<SocialMemberSessionCache>()
        val service = AuthService(
            memberRepository,
            emailVerificationCache,
            mailSender,
            tokenProvider,
            passwordEncoder,
            refreshTokenCache,
            socialMemberSessionCache
        )

        val refreshToken = "refresh-token"
        val principal = AuthPrincipal.refreshToken(1L)

        every { tokenProvider.decodeToken(refreshToken) } returns principal
        coEvery { refreshTokenCache.get(principal.memberId) } returns refreshToken
        coEvery { memberRepository.findById(principal.memberId) } returns null

        shouldThrow<UnauthorizedException> {
            service.reissue(refreshToken)
        }
    }

    @Test
    fun `리프레시 토큰 재발급 시 새 토큰을 발급하고 캐시에 저장한다`() = runTest {
        val memberRepository = mockk<MemberRepository>()
        val emailVerificationCache = mockk<EmailVerificationCache>()
        val mailSender = mockk<MailSender>(relaxed = true)
        val tokenProvider = mockk<TokenProvider>()
        val passwordEncoder = mockk<PasswordEncoder>()
        val refreshTokenCache = mockk<RefreshTokenCache>()
        val socialMemberSessionCache = mockk<SocialMemberSessionCache>()
        val service = AuthService(
            memberRepository,
            emailVerificationCache,
            mailSender,
            tokenProvider,
            passwordEncoder,
            refreshTokenCache,
            socialMemberSessionCache
        )

        val refreshToken = "refresh-token"
        val principal = AuthPrincipal.refreshToken(1L)
        val member = createMember(id = principal.memberId)

        every { tokenProvider.decodeToken(refreshToken) } returns principal
        coEvery { refreshTokenCache.get(principal.memberId) } returns refreshToken
        coEvery { memberRepository.findById(principal.memberId) } returns member
        every { tokenProvider.encodeToken(any(), any()) } returnsMany listOf("new-access", "new-refresh")
        coEvery { refreshTokenCache.set(member.id, "new-refresh", 604_800_000L) } returns Unit

        val result = service.reissue(refreshToken)

        result.accessToken shouldBe "new-access"
        result.refreshToken shouldBe "new-refresh"
        verify(exactly = 1) { tokenProvider.decodeToken(refreshToken) }
        coVerify(exactly = 1) { refreshTokenCache.set(member.id, "new-refresh", 604_800_000L) }
    }

    @Test
    fun `토큰 발급 시 액세스 토큰과 리프레시 토큰을 생성한다`() {
        val memberRepository = mockk<MemberRepository>()
        val emailVerificationCache = mockk<EmailVerificationCache>()
        val mailSender = mockk<MailSender>(relaxed = true)
        val tokenProvider = mockk<TokenProvider>()
        val passwordEncoder = mockk<PasswordEncoder>()
        val refreshTokenCache = mockk<RefreshTokenCache>()
        val socialMemberSessionCache = mockk<SocialMemberSessionCache>()
        val service = AuthService(
            memberRepository,
            emailVerificationCache,
            mailSender,
            tokenProvider,
            passwordEncoder,
            refreshTokenCache,
            socialMemberSessionCache
        )

        every { tokenProvider.encodeToken(any(), any()) } returnsMany listOf("access-token", "refresh-token")

        val result = service.issueTokens(memberId = 1L, role = "USER")

        result.accessToken shouldBe "access-token"
        result.refreshToken shouldBe "refresh-token"
        verify(exactly = 1) {
            tokenProvider.encodeToken(
                match { it.type == TokenType.ACCESS && it.memberId == 1L && it.role == "USER" },
                3_600_000L
            )
        }
        verify(exactly = 1) {
            tokenProvider.encodeToken(
                match { it.type == TokenType.REFRESH && it.memberId == 1L },
                604_800_000L
            )
        }
    }

    @Test
    fun `로그아웃 시 리프레시 토큰을 삭제한다`() = runTest {
        val memberRepository = mockk<MemberRepository>()
        val emailVerificationCache = mockk<EmailVerificationCache>()
        val mailSender = mockk<MailSender>(relaxed = true)
        val tokenProvider = mockk<TokenProvider>()
        val passwordEncoder = mockk<PasswordEncoder>()
        val refreshTokenCache = mockk<RefreshTokenCache>()
        val socialMemberSessionCache = mockk<SocialMemberSessionCache>()
        val service = AuthService(
            memberRepository,
            emailVerificationCache,
            mailSender,
            tokenProvider,
            passwordEncoder,
            refreshTokenCache,
            socialMemberSessionCache
        )

        val memberId = 1L

        coEvery { refreshTokenCache.evict(memberId) } returns Unit

        val result = service.logout(memberId)

        result.success shouldBe true
        coVerify(exactly = 1) { refreshTokenCache.evict(memberId) }
    }

    @Test
    fun `소셜 세션 로그인 시 세션이 없으면 예외가 발생한다`() = runTest {
        val memberRepository = mockk<MemberRepository>()
        val emailVerificationCache = mockk<EmailVerificationCache>()
        val mailSender = mockk<MailSender>(relaxed = true)
        val tokenProvider = mockk<TokenProvider>()
        val passwordEncoder = mockk<PasswordEncoder>()
        val refreshTokenCache = mockk<RefreshTokenCache>()
        val socialMemberSessionCache = mockk<SocialMemberSessionCache>()
        val service = AuthService(
            memberRepository,
            emailVerificationCache,
            mailSender,
            tokenProvider,
            passwordEncoder,
            refreshTokenCache,
            socialMemberSessionCache
        )

        val command = LoginSocialSessionCommand(token = "token", memberId = 1L)

        coEvery { socialMemberSessionCache.get(command.token) } returns null

        shouldThrow<UnauthorizedException> {
            service.loginSocialSession(command)
        }
    }

    @Test
    fun `소셜 세션 로그인 시 멤버 아이디가 다르면 예외가 발생한다`() = runTest {
        val memberRepository = mockk<MemberRepository>()
        val emailVerificationCache = mockk<EmailVerificationCache>()
        val mailSender = mockk<MailSender>(relaxed = true)
        val tokenProvider = mockk<TokenProvider>()
        val passwordEncoder = mockk<PasswordEncoder>()
        val refreshTokenCache = mockk<RefreshTokenCache>()
        val socialMemberSessionCache = mockk<SocialMemberSessionCache>()
        val service = AuthService(
            memberRepository,
            emailVerificationCache,
            mailSender,
            tokenProvider,
            passwordEncoder,
            refreshTokenCache,
            socialMemberSessionCache
        )

        val command = LoginSocialSessionCommand(token = "token", memberId = 1L)

        coEvery { socialMemberSessionCache.get(command.token) } returns 2L

        shouldThrow<UnauthorizedException> {
            service.loginSocialSession(command)
        }
    }

    @Test
    fun `소셜 세션 로그인 시 사용자가 없으면 예외가 발생한다`() = runTest {
        val memberRepository = mockk<MemberRepository>()
        val emailVerificationCache = mockk<EmailVerificationCache>()
        val mailSender = mockk<MailSender>(relaxed = true)
        val tokenProvider = mockk<TokenProvider>()
        val passwordEncoder = mockk<PasswordEncoder>()
        val refreshTokenCache = mockk<RefreshTokenCache>()
        val socialMemberSessionCache = mockk<SocialMemberSessionCache>()
        val service = AuthService(
            memberRepository,
            emailVerificationCache,
            mailSender,
            tokenProvider,
            passwordEncoder,
            refreshTokenCache,
            socialMemberSessionCache
        )

        val command = LoginSocialSessionCommand(token = "token", memberId = 1L)

        coEvery { socialMemberSessionCache.get(command.token) } returns command.memberId
        coEvery { memberRepository.findById(command.memberId) } returns null

        shouldThrow<UnauthorizedException> {
            service.loginSocialSession(command)
        }
    }

    @Test
    fun `소셜 세션 로그인 시 토큰을 발급한다`() = runTest {
        val memberRepository = mockk<MemberRepository>()
        val emailVerificationCache = mockk<EmailVerificationCache>()
        val mailSender = mockk<MailSender>(relaxed = true)
        val tokenProvider = mockk<TokenProvider>()
        val passwordEncoder = mockk<PasswordEncoder>()
        val refreshTokenCache = mockk<RefreshTokenCache>()
        val socialMemberSessionCache = mockk<SocialMemberSessionCache>()
        val service = AuthService(
            memberRepository,
            emailVerificationCache,
            mailSender,
            tokenProvider,
            passwordEncoder,
            refreshTokenCache,
            socialMemberSessionCache
        )

        val command = LoginSocialSessionCommand(token = "token", memberId = 1L)
        val member = createMember(id = command.memberId)

        coEvery { socialMemberSessionCache.get(command.token) } returns command.memberId
        coEvery { memberRepository.findById(command.memberId) } returns member
        every { tokenProvider.encodeToken(any(), any()) } returnsMany listOf("access-token", "refresh-token")

        val result = service.loginSocialSession(command)

        result.accessToken shouldBe "access-token"
        result.refreshToken shouldBe "refresh-token"
        verify(exactly = 1) {
            tokenProvider.encodeToken(
                match {
                    it.type == TokenType.ACCESS &&
                        it.memberId == member.id &&
                        it.role == member.role.name
                },
                3_600_000L
            )
        }
        verify(exactly = 1) {
            tokenProvider.encodeToken(
                match { it.type == TokenType.REFRESH && it.memberId == member.id },
                604_800_000L
            )
        }
    }
}

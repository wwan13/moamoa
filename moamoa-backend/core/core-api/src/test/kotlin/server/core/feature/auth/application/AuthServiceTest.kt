package server.core.feature.auth.application

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import server.core.feature.auth.infra.RefreshTokenCache
import server.core.feature.auth.infra.SocialMemberSessionCache
import server.core.feature.member.domain.MemberRepository
import server.core.fixture.createMember
import server.core.global.security.UnauthorizedException
import server.password.PasswordEncoder
import server.token.AuthPrincipal
import server.token.TokenProvider
import server.token.TokenType
import test.UnitTest
import java.util.Optional

class AuthServiceTest : UnitTest() {
    @Test
    fun `로그인 시 존재하지 않는 사용자면 예외가 발생한다`() = runTest {
        val fixture = createFixture()
        val command = LoginCommand(email = "none@example.com", password = "password!!")

        coEvery { fixture.memberRepository.findByEmail(command.email) } returns null

        val exception = shouldThrow<NoSuchElementException> {
            fixture.service.login(command)
        }

        exception.message shouldBe "존재하지 않는 사용자 입니다."
        verify(exactly = 0) { fixture.passwordEncoder.matches(any(), any()) }
    }

    @Test
    fun `로그인 시 비밀번호가 다르면 예외가 발생한다`() = runTest {
        val fixture = createFixture()
        val command = LoginCommand(email = "user@example.com", password = "password!!")
        val member = createMember(id = 1L, email = command.email, password = "encoded")

        coEvery { fixture.memberRepository.findByEmail(command.email) } returns member
        every { fixture.passwordEncoder.matches(command.password, member.password) } returns false

        val exception = shouldThrow<IllegalArgumentException> {
            fixture.service.login(command)
        }

        exception.message shouldBe "비밀번호가 일치하지 않습니다."
        verify(exactly = 0) { fixture.tokenProvider.encodeToken(any(), any()) }
        coVerify(exactly = 0) { fixture.refreshTokenCache.set(any(), any(), any()) }
    }

    @Test
    fun `로그인 시 토큰을 발급하고 리프레시 토큰을 저장한다`() = runTest {
        val fixture = createFixture()
        val command = LoginCommand(email = "user@example.com", password = "password!!")
        val member = createMember(id = 1L, email = command.email, password = "encoded")

        coEvery { fixture.memberRepository.findByEmail(command.email) } returns member
        every { fixture.passwordEncoder.matches(command.password, member.password) } returns true
        every { fixture.tokenProvider.encodeToken(any(), any()) } returnsMany listOf("access-token", "refresh-token")
        coEvery { fixture.refreshTokenCache.set(member.id, "refresh-token", 604_800_000L) } returns Unit

        val result = fixture.service.login(command)

        result.accessToken shouldBe "access-token"
        result.refreshToken shouldBe "refresh-token"
        verify(exactly = 1) {
            fixture.tokenProvider.encodeToken(
                match {
                    it.type == TokenType.ACCESS &&
                        it.memberId == member.id &&
                        it.role == member.role.name
                },
                3_600_000L
            )
        }
        verify(exactly = 1) {
            fixture.tokenProvider.encodeToken(
                match {
                    it.type == TokenType.REFRESH &&
                        it.memberId == member.id
                },
                604_800_000L
            )
        }
        coVerify(exactly = 1) { fixture.refreshTokenCache.set(member.id, "refresh-token", 604_800_000L) }
    }

    @Test
    fun `리프레시 토큰 재발급 시 토큰 타입이 다르면 예외가 발생한다`() = runTest {
        val fixture = createFixture()
        val refreshToken = "refresh-token"

        every { fixture.tokenProvider.decodeToken(refreshToken) } returns AuthPrincipal.accessToken(1L, "USER")

        shouldThrow<UnauthorizedException> {
            fixture.service.reissue(refreshToken)
        }
    }

    @Test
    fun `리프레시 토큰 재발급 시 저장된 토큰이 없으면 예외가 발생한다`() = runTest {
        val fixture = createFixture()
        val refreshToken = "refresh-token"
        val principal = AuthPrincipal.refreshToken(1L)

        every { fixture.tokenProvider.decodeToken(refreshToken) } returns principal
        coEvery { fixture.refreshTokenCache.get(principal.memberId) } returns null

        shouldThrow<UnauthorizedException> {
            fixture.service.reissue(refreshToken)
        }
    }

    @Test
    fun `리프레시 토큰 재발급 시 토큰이 일치하지 않으면 예외가 발생한다`() = runTest {
        val fixture = createFixture()
        val refreshToken = "refresh-token"
        val principal = AuthPrincipal.refreshToken(1L)

        every { fixture.tokenProvider.decodeToken(refreshToken) } returns principal
        coEvery { fixture.refreshTokenCache.get(principal.memberId) } returns "other-token"

        shouldThrow<UnauthorizedException> {
            fixture.service.reissue(refreshToken)
        }
    }

    @Test
    fun `리프레시 토큰 재발급 시 사용자가 없으면 예외가 발생한다`() = runTest {
        val fixture = createFixture()
        val refreshToken = "refresh-token"
        val principal = AuthPrincipal.refreshToken(1L)

        every { fixture.tokenProvider.decodeToken(refreshToken) } returns principal
        coEvery { fixture.refreshTokenCache.get(principal.memberId) } returns refreshToken
        coEvery { fixture.memberRepository.findById(principal.memberId) } returns Optional.empty()

        shouldThrow<UnauthorizedException> {
            fixture.service.reissue(refreshToken)
        }
    }

    @Test
    fun `리프레시 토큰 재발급 시 새 토큰을 발급하고 캐시에 저장한다`() = runTest {
        val fixture = createFixture()
        val refreshToken = "refresh-token"
        val principal = AuthPrincipal.refreshToken(1L)
        val member = createMember(id = principal.memberId)

        every { fixture.tokenProvider.decodeToken(refreshToken) } returns principal
        coEvery { fixture.refreshTokenCache.get(principal.memberId) } returns refreshToken
        coEvery { fixture.memberRepository.findById(principal.memberId) } returns Optional.of(member)
        every { fixture.tokenProvider.encodeToken(any(), any()) } returnsMany listOf("new-access", "new-refresh")
        coEvery { fixture.refreshTokenCache.set(member.id, "new-refresh", 604_800_000L) } returns Unit

        val result = fixture.service.reissue(refreshToken)

        result.accessToken shouldBe "new-access"
        result.refreshToken shouldBe "new-refresh"
        verify(exactly = 1) { fixture.tokenProvider.decodeToken(refreshToken) }
        coVerify(exactly = 1) { fixture.refreshTokenCache.set(member.id, "new-refresh", 604_800_000L) }
    }

    @Test
    fun `토큰 발급 시 액세스 토큰과 리프레시 토큰을 생성한다`() {
        val fixture = createFixture()

        every { fixture.tokenProvider.encodeToken(any(), any()) } returnsMany listOf("access-token", "refresh-token")

        val result = fixture.service.issueTokens(memberId = 1L, role = "USER")

        result.accessToken shouldBe "access-token"
        result.refreshToken shouldBe "refresh-token"
        verify(exactly = 1) {
            fixture.tokenProvider.encodeToken(
                match { it.type == TokenType.ACCESS && it.memberId == 1L && it.role == "USER" },
                3_600_000L
            )
        }
        verify(exactly = 1) {
            fixture.tokenProvider.encodeToken(
                match { it.type == TokenType.REFRESH && it.memberId == 1L },
                604_800_000L
            )
        }
    }

    @Test
    fun `로그아웃 시 리프레시 토큰을 삭제한다`() = runTest {
        val fixture = createFixture()
        val memberId = 1L

        coEvery { fixture.refreshTokenCache.evict(memberId) } returns Unit

        fixture.service.logout(memberId)

        coVerify(exactly = 1) { fixture.refreshTokenCache.evict(memberId) }
    }

    @Test
    fun `소셜 세션 로그인 시 세션이 없으면 예외가 발생한다`() = runTest {
        val fixture = createFixture()
        val command = LoginSocialSessionCommand(token = "token", memberId = 1L)

        coEvery { fixture.socialMemberSessionCache.get(command.token) } returns null

        shouldThrow<UnauthorizedException> {
            fixture.service.loginSocialSession(command)
        }
    }

    @Test
    fun `소셜 세션 로그인 시 멤버 아이디가 다르면 예외가 발생한다`() = runTest {
        val fixture = createFixture()
        val command = LoginSocialSessionCommand(token = "token", memberId = 1L)

        coEvery { fixture.socialMemberSessionCache.get(command.token) } returns 2L

        shouldThrow<UnauthorizedException> {
            fixture.service.loginSocialSession(command)
        }
    }

    @Test
    fun `소셜 세션 로그인 시 사용자가 없으면 예외가 발생한다`() = runTest {
        val fixture = createFixture()
        val command = LoginSocialSessionCommand(token = "token", memberId = 1L)

        coEvery { fixture.socialMemberSessionCache.get(command.token) } returns command.memberId
        coEvery { fixture.memberRepository.findById(command.memberId) } returns Optional.empty()

        shouldThrow<UnauthorizedException> {
            fixture.service.loginSocialSession(command)
        }
    }

    @Test
    fun `소셜 세션 로그인 시 토큰을 발급한다`() = runTest {
        val fixture = createFixture()
        val command = LoginSocialSessionCommand(token = "token", memberId = 1L)
        val member = createMember(id = command.memberId)

        coEvery { fixture.socialMemberSessionCache.get(command.token) } returns command.memberId
        coEvery { fixture.memberRepository.findById(command.memberId) } returns Optional.of(member)
        every { fixture.tokenProvider.encodeToken(any(), any()) } returnsMany listOf("access-token", "refresh-token")

        val result = fixture.service.loginSocialSession(command)

        result.accessToken shouldBe "access-token"
        result.refreshToken shouldBe "refresh-token"
        verify(exactly = 1) {
            fixture.tokenProvider.encodeToken(
                match {
                    it.type == TokenType.ACCESS &&
                        it.memberId == member.id &&
                        it.role == member.role.name
                },
                3_600_000L
            )
        }
        verify(exactly = 1) {
            fixture.tokenProvider.encodeToken(
                match { it.type == TokenType.REFRESH && it.memberId == member.id },
                604_800_000L
            )
        }
    }

    private fun createFixture(): Fixture {
        val memberRepository = mockk<MemberRepository>()
        val tokenProvider = mockk<TokenProvider>()
        val passwordEncoder = mockk<PasswordEncoder>()
        val refreshTokenCache = mockk<RefreshTokenCache>()
        val socialMemberSessionCache = mockk<SocialMemberSessionCache>()

        val service = AuthService(
            memberRepository = memberRepository,
            tokenProvider = tokenProvider,
            passwordEncoder = passwordEncoder,
            refreshTokenCache = refreshTokenCache,
            socialMemberSessionCache = socialMemberSessionCache
        )

        return Fixture(
            service = service,
            memberRepository = memberRepository,
            tokenProvider = tokenProvider,
            passwordEncoder = passwordEncoder,
            refreshTokenCache = refreshTokenCache,
            socialMemberSessionCache = socialMemberSessionCache
        )
    }

    private data class Fixture(
        val service: AuthService,
        val memberRepository: MemberRepository,
        val tokenProvider: TokenProvider,
        val passwordEncoder: PasswordEncoder,
        val refreshTokenCache: RefreshTokenCache,
        val socialMemberSessionCache: SocialMemberSessionCache
    )
}

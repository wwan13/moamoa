package server.core.feature.member.command.application

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
import server.core.feature.auth.infra.SocialMemberSessionCache
import server.core.feature.member.application.ChangePasswordCommand
import server.core.feature.member.application.CreateInternalMemberCommand
import server.core.feature.member.application.CreateSocialMemberCommand
import server.core.feature.member.application.EmailExistsCommand
import server.core.feature.member.application.MemberService
import server.core.feature.member.domain.Member
import server.core.feature.member.domain.MemberCreateEvent
import server.core.feature.member.domain.MemberRepository
import server.core.feature.member.domain.Provider
import server.core.fixture.createMember
import server.core.global.security.Passport
import server.core.global.security.UnauthorizedException
import server.core.infra.event.TransactionalEventPublisher
import server.password.PasswordEncoder
import test.UnitTest
import java.util.Optional

class MemberServiceTest : UnitTest() {
    @Test
    fun `내부 회원가입 시 비밀번호가 다르면 예외가 발생한다`() = runTest {
        val fixture = createFixture()
        val command = CreateInternalMemberCommand(
            email = "user@example.com",
            password = "password!1",
            passwordConfirm = "password!2"
        )

        val exception = shouldThrow<IllegalArgumentException> {
            fixture.service.createInternalMember(command)
        }

        exception.message shouldBe "비밀번호가 일치하지 않습니다."
    }

    @Test
    fun `내부 회원가입 시 이미 가입된 이메일이면 예외가 발생한다`() = runTest {
        val fixture = createFixture()
        val command = CreateInternalMemberCommand(
            email = "user@example.com",
            password = "password!1",
            passwordConfirm = "password!1"
        )

        every { fixture.passwordEncoder.encode(command.password) } returns "encoded-password"
        coEvery { fixture.memberRepository.existsByEmail(command.email) } returns true

        val exception = shouldThrow<IllegalArgumentException> {
            fixture.service.createInternalMember(command)
        }

        exception.message shouldBe "이미 가입된 이메일 입니다."
        coVerify(exactly = 0) { fixture.memberRepository.save(any()) }
        verify(exactly = 0) { fixture.eventPublisher.publish(any(), any()) }
    }

    @Test
    fun `내부 회원가입 시 회원을 저장하고 생성 이벤트를 등록한다`() = runTest {
        val fixture = createFixture()
        val command = CreateInternalMemberCommand(
            email = "user@example.com",
            password = "password!1",
            passwordConfirm = "password!1"
        )
        val savedMember = createMember(id = 10L, email = command.email, password = "encoded-password")
        val savedSlot = slot<Member>()

        every { fixture.passwordEncoder.encode(command.password) } returns "encoded-password"
        coEvery { fixture.memberRepository.existsByEmail(command.email) } returns false
        coEvery { fixture.memberRepository.save(capture(savedSlot)) } returns savedMember

        val result = fixture.service.createInternalMember(command)

        result.id shouldBe savedMember.id
        result.email shouldBe savedMember.email
        result.role shouldBe savedMember.role
        savedSlot.captured.email shouldBe command.email
        savedSlot.captured.password shouldBe "encoded-password"
        savedSlot.captured.provider shouldBe Provider.INTERNAL
        verify(exactly = 1) {
            fixture.eventPublisher.publish(
                match<MemberCreateEvent> {
                    it.memberId == savedMember.id && it.email == savedMember.email
                },
                any()
            )
        }
    }

    @Test
    fun `소셜 회원가입 시 provider가 INTERNAL이면 예외가 발생한다`() = runTest {
        val fixture = createFixture()
        val command = CreateSocialMemberCommand(
            email = "user@example.com",
            provider = Provider.INTERNAL,
            providerKey = "internal-key"
        )

        val exception = shouldThrow<IllegalStateException> {
            fixture.service.createSocialMember(command)
        }

        exception.message shouldBe "소셜 로그인으로 회원가입한 유저입니다."
        coVerify(exactly = 0) { fixture.memberRepository.save(any()) }
    }

    @Test
    fun `소셜 회원가입 시 이미 가입된 이메일이면 예외가 발생한다`() = runTest {
        val fixture = createFixture()
        val command = CreateSocialMemberCommand(
            email = "user@example.com",
            provider = Provider.GITHUB,
            providerKey = "github-key"
        )

        coEvery { fixture.memberRepository.existsByEmail(command.email) } returns true

        val exception = shouldThrow<IllegalArgumentException> {
            fixture.service.createSocialMember(command)
        }

        exception.message shouldBe "이미 가입된 이메일 입니다."
        coVerify(exactly = 0) { fixture.memberRepository.save(any()) }
        verify(exactly = 0) { fixture.eventPublisher.publish(any(), any()) }
    }

    @Test
    fun `소셜 회원가입 시 회원을 저장하고 생성 이벤트를 등록한다`() = runTest {
        val fixture = createFixture()
        val command = CreateSocialMemberCommand(
            email = "social@example.com",
            provider = Provider.GITHUB,
            providerKey = "github-key"
        )
        val savedMember = createMember(
            id = 12L,
            email = command.email,
            provider = command.provider,
            providerKey = command.providerKey,
            password = ""
        )
        val savedSlot = slot<Member>()

        coEvery { fixture.memberRepository.existsByEmail(command.email) } returns false
        coEvery { fixture.memberRepository.save(capture(savedSlot)) } returns savedMember

        val result = fixture.service.createSocialMember(command)

        result.id shouldBe savedMember.id
        result.email shouldBe savedMember.email
        result.role shouldBe savedMember.role
        savedSlot.captured.email shouldBe command.email
        savedSlot.captured.provider shouldBe command.provider
        savedSlot.captured.providerKey shouldBe command.providerKey
        verify(exactly = 1) {
            fixture.eventPublisher.publish(
                match<MemberCreateEvent> {
                    it.memberId == savedMember.id && it.email == savedMember.email
                },
                any()
            )
        }
    }

    @Test
    fun `소셜 회원가입 세션 생성 시 토큰을 저장한다`() = runTest {
        val fixture = createFixture()
        val command = CreateSocialMemberCommand(
            email = "social@example.com",
            provider = Provider.GITHUB,
            providerKey = "github-key"
        )
        val savedMember = createMember(
            id = 15L,
            email = command.email,
            provider = command.provider,
            providerKey = command.providerKey,
            password = ""
        )

        coEvery { fixture.memberRepository.existsByEmail(command.email) } returns false
        coEvery { fixture.memberRepository.save(any()) } returns savedMember
        coEvery { fixture.socialMemberSessionCache.set(any(), any()) } returns Unit

        val result = fixture.service.createSocialMemberWithSession(command)

        result.member.id shouldBe savedMember.id
        result.token.isNotBlank() shouldBe true
        coVerify(exactly = 1) { fixture.socialMemberSessionCache.set(result.token, savedMember.id) }
        verify(exactly = 1) {
            fixture.eventPublisher.publish(
                match<MemberCreateEvent> {
                    it.memberId == savedMember.id && it.email == savedMember.email
                },
                any()
            )
        }
    }

    @Test
    fun `소셜 회원가입 세션 생성 시 회원가입 실패면 토큰을 저장하지 않는다`() = runTest {
        val fixture = createFixture()
        val command = CreateSocialMemberCommand(
            email = "social@example.com",
            provider = Provider.GITHUB,
            providerKey = "github-key"
        )

        coEvery { fixture.memberRepository.existsByEmail(command.email) } returns true

        shouldThrow<IllegalArgumentException> {
            fixture.service.createSocialMemberWithSession(command)
        }

        coVerify(exactly = 0) { fixture.socialMemberSessionCache.set(any(), any()) }
        verify(exactly = 0) { fixture.eventPublisher.publish(any(), any()) }
    }

    @Test
    fun `회원 조회 시 존재하면 MemberData를 반환한다`() = runTest {
        val fixture = createFixture()
        val memberId = 101L
        val member = createMember(id = memberId, email = "user@example.com")

        coEvery { fixture.memberRepository.findById(memberId) } returns Optional.of(member)

        val result = fixture.service.findById(memberId)

        result?.id shouldBe memberId
        result?.email shouldBe member.email
        result?.role shouldBe member.role
    }

    @Test
    fun `회원 조회 시 존재하지 않으면 예외가 발생한다`() = runTest {
        val fixture = createFixture()
        val memberId = 101L

        coEvery { fixture.memberRepository.findById(memberId) } returns Optional.empty()

        val exception = shouldThrow<NoSuchElementException> {
            fixture.service.findById(memberId)
        }

        exception.message shouldBe "존재하지 않는 사용자 입니다."
    }

    @Test
    fun `소셜 회원 조회 시 존재하면 MemberData를 반환한다`() = runTest {
        val fixture = createFixture()
        val member = createMember(
            id = 202L,
            email = "social@example.com",
            provider = Provider.GITHUB,
            providerKey = "github-key",
            password = ""
        )

        coEvery {
            fixture.memberRepository.findByProviderAndProviderKey(member.provider, member.providerKey)
        } returns member

        val result = fixture.service.findSocialMember(member.provider, member.providerKey)

        result.id shouldBe member.id
        result.email shouldBe member.email
        result.role shouldBe member.role
    }

    @Test
    fun `소셜 회원 조회 시 존재하지 않으면 예외가 발생한다`() = runTest {
        val fixture = createFixture()

        coEvery {
            fixture.memberRepository.findByProviderAndProviderKey(Provider.GITHUB, "github-key")
        } returns null

        val exception = shouldThrow<NoSuchElementException> {
            fixture.service.findSocialMember(Provider.GITHUB, "github-key")
        }

        exception.message shouldBe "존재하지 않는 사용자 입니다."
    }

    @Test
    fun `이메일 존재 여부 조회 시 exists가 true이면 true를 반환한다`() = runTest {
        val fixture = createFixture()
        val command = EmailExistsCommand(email = "user@example.com")

        coEvery { fixture.memberRepository.existsByEmail(command.email) } returns true

        val result = fixture.service.emailExists(command)

        result.exists shouldBe true
    }

    @Test
    fun `이메일 존재 여부 조회 시 exists가 false이면 false를 반환한다`() = runTest {
        val fixture = createFixture()
        val command = EmailExistsCommand(email = "user@example.com")

        coEvery { fixture.memberRepository.existsByEmail(command.email) } returns false

        val result = fixture.service.emailExists(command)

        result.exists shouldBe false
    }

    @Test
    fun `비밀번호 변경 시 동일한 비밀번호이면 예외가 발생한다`() = runTest {
        val fixture = createFixture()
        val command = ChangePasswordCommand(
            oldPassword = "password!1",
            newPassword = "password!1",
            passwordConfirm = "password!1"
        )
        val passport = Passport(memberId = 1L, role = createMember().role)

        val exception = shouldThrow<IllegalArgumentException> {
            fixture.service.changePassword(command, passport)
        }

        exception.message shouldBe "같은 비밀번호는 사용할 수 없습니다."
    }

    @Test
    fun `비밀번호 변경 시 비밀번호 확인이 다르면 예외가 발생한다`() = runTest {
        val fixture = createFixture()
        val command = ChangePasswordCommand(
            oldPassword = "password!1",
            newPassword = "password!2",
            passwordConfirm = "password!3"
        )
        val passport = Passport(memberId = 1L, role = createMember().role)

        val exception = shouldThrow<IllegalArgumentException> {
            fixture.service.changePassword(command, passport)
        }

        exception.message shouldBe "비밀번호가 일치하지 않습니다."
    }

    @Test
    fun `비밀번호 변경 시 사용자가 없으면 인증 예외가 발생한다`() = runTest {
        val fixture = createFixture()
        val command = ChangePasswordCommand(
            oldPassword = "password!1",
            newPassword = "password!2",
            passwordConfirm = "password!2"
        )
        val passport = Passport(memberId = 404L, role = createMember().role)

        coEvery { fixture.memberRepository.findById(passport.memberId) } returns Optional.empty()

        shouldThrow<UnauthorizedException> {
            fixture.service.changePassword(command, passport)
        }
    }

    @Test
    fun `비밀번호 변경 시 내부 회원이 아니면 예외가 발생한다`() = runTest {
        val fixture = createFixture()
        val command = ChangePasswordCommand(
            oldPassword = "password!1",
            newPassword = "password!2",
            passwordConfirm = "password!2"
        )
        val member = createMember(id = 1L, provider = Provider.GITHUB, providerKey = "github-key", password = "")
        val passport = Passport(memberId = member.id, role = member.role)

        coEvery { fixture.memberRepository.findById(passport.memberId) } returns Optional.of(member)

        val exception = shouldThrow<IllegalArgumentException> {
            fixture.service.changePassword(command, passport)
        }

        exception.message shouldBe "이메일로 회원가입한 사용자가 아닙니다."
    }

    @Test
    fun `비밀번호 변경 시 기존 비밀번호가 다르면 예외가 발생한다`() = runTest {
        val fixture = createFixture()
        val command = ChangePasswordCommand(
            oldPassword = "password!1",
            newPassword = "password!2",
            passwordConfirm = "password!2"
        )
        val member = createMember(id = 1L, password = "encoded-old")
        val passport = Passport(memberId = member.id, role = member.role)

        coEvery { fixture.memberRepository.findById(passport.memberId) } returns Optional.of(member)
        every { fixture.passwordEncoder.matches(command.oldPassword, member.password) } returns false

        val exception = shouldThrow<IllegalArgumentException> {
            fixture.service.changePassword(command, passport)
        }

        exception.message shouldBe "기존 비밀번호가 일치하지 않습니다."
    }

    @Test
    fun `비밀번호 변경 시 새 비밀번호로 저장한다`() = runTest {
        val fixture = createFixture()
        val command = ChangePasswordCommand(
            oldPassword = "password!1",
            newPassword = "password!2",
            passwordConfirm = "password!2"
        )
        val member = createMember(id = 1L, password = "encoded-old")
        val passport = Passport(memberId = member.id, role = member.role)

        coEvery { fixture.memberRepository.findById(passport.memberId) } returns Optional.of(member)
        every { fixture.passwordEncoder.matches(command.oldPassword, member.password) } returns true
        every { fixture.passwordEncoder.encode(command.newPassword) } returns "encoded-new"

        fixture.service.changePassword(command, passport)

        member.password shouldBe "encoded-new"
    }

    private fun createFixture(): Fixture {
        val memberRepository = mockk<MemberRepository>()
        val passwordEncoder = mockk<PasswordEncoder>()
        val socialMemberSessionCache = mockk<SocialMemberSessionCache>()
        val eventPublisher = mockk<TransactionalEventPublisher>(relaxed = true)

        val service = MemberService(
            memberRepository = memberRepository,
            passwordEncoder = passwordEncoder,
            socialMemberSessionCache = socialMemberSessionCache,
            eventPublisher = eventPublisher,
        )

        return Fixture(
            service = service,
            memberRepository = memberRepository,
            passwordEncoder = passwordEncoder,
            socialMemberSessionCache = socialMemberSessionCache,
            eventPublisher = eventPublisher,
        )
    }

    private data class Fixture(
        val service: MemberService,
        val memberRepository: MemberRepository,
        val passwordEncoder: PasswordEncoder,
        val socialMemberSessionCache: SocialMemberSessionCache,
        val eventPublisher: TransactionalEventPublisher,
    )
}

package server.feature.member.command.application

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import server.feature.member.command.domain.MemberCreateEvent
import server.feature.member.command.domain.MemberRepository
import server.feature.member.command.domain.Provider
import server.fixture.createMember
import server.infra.cache.EmailVerificationCache
import server.infra.cache.SocialMemberSessionCache
import server.infra.db.transaction.TransactionScope
import server.infra.db.transaction.Transactional
import server.shared.security.password.PasswordEncoder
import server.security.Passport
import server.security.UnauthorizedException
import test.UnitTest

class MemberServiceTest : UnitTest() {
    @Test
    fun `내부 회원가입 시 비밀번호가 다르면 예외가 발생한다`() = runTest {
        val transactional = mockk<Transactional>()
        val memberRepository = mockk<MemberRepository>()
        val emailVerificationCache = mockk<EmailVerificationCache>()
        val passwordEncoder = mockk<PasswordEncoder>()
        val socialMemberSessionCache = mockk<SocialMemberSessionCache>()
        val service = MemberService(
            transactional,
            memberRepository,
            emailVerificationCache,
            passwordEncoder,
            socialMemberSessionCache
        )
        val command = CreateInternalMemberCommand(
            email = "user@example.com",
            password = "password!1",
            passwordConfirm = "password!2"
        )

        val exception = shouldThrow<IllegalArgumentException> {
            service.createInternalMember(command)
        }

        exception.message shouldBe "비밀번호가 일치하지 않습니다."
        coVerify(exactly = 0) { transactional.invoke<MemberData>(any(), any()) }
    }

    @Test
    fun `내부 회원가입 시 이미 가입된 이메일이면 예외가 발생한다`() = runTest {
        val transactional = mockk<Transactional>()
        val memberRepository = mockk<MemberRepository>()
        val emailVerificationCache = mockk<EmailVerificationCache>()
        val passwordEncoder = mockk<PasswordEncoder>()
        val socialMemberSessionCache = mockk<SocialMemberSessionCache>()
        val transactionScope = mockk<TransactionScope>(relaxed = true)
        val service = MemberService(
            transactional,
            memberRepository,
            emailVerificationCache,
            passwordEncoder,
            socialMemberSessionCache
        )
        val command = CreateInternalMemberCommand(
            email = "user@example.com",
            password = "password!1",
            passwordConfirm = "password!1"
        )

        every { passwordEncoder.encode(command.password) } returns "encoded-password"
        coEvery { memberRepository.existsByEmail(command.email) } returns true
        coEvery { transactional.invoke<MemberData>(any(), any()) } coAnswers {
            val block = secondArg<suspend TransactionScope.() -> MemberData>()
            block(transactionScope)
        }

        val exception = shouldThrow<IllegalArgumentException> {
            service.createInternalMember(command)
        }

        exception.message shouldBe "이미 가입된 이메일 입니다."
        coVerify(exactly = 0) { memberRepository.save(any()) }
    }

    @Test
    fun `내부 회원가입 시 회원을 저장하고 생성 이벤트를 등록한다`() = runTest {
        val transactional = mockk<Transactional>()
        val memberRepository = mockk<MemberRepository>()
        val emailVerificationCache = mockk<EmailVerificationCache>()
        val passwordEncoder = mockk<PasswordEncoder>()
        val socialMemberSessionCache = mockk<SocialMemberSessionCache>()
        val transactionScope = mockk<TransactionScope>(relaxed = true)
        val service = MemberService(
            transactional,
            memberRepository,
            emailVerificationCache,
            passwordEncoder,
            socialMemberSessionCache
        )
        val command = CreateInternalMemberCommand(
            email = "user@example.com",
            password = "password!1",
            passwordConfirm = "password!1"
        )
        val savedMember = createMember(id = 10L, email = command.email, password = "encoded-password")
        val savedSlot = slot<server.feature.member.command.domain.Member>()

        every { passwordEncoder.encode(command.password) } returns "encoded-password"
        coEvery { memberRepository.existsByEmail(command.email) } returns false
        coEvery { memberRepository.save(capture(savedSlot)) } returns savedMember
        coEvery { transactional.invoke<MemberData>(any(), any()) } coAnswers {
            val block = secondArg<suspend TransactionScope.() -> MemberData>()
            block(transactionScope)
        }

        val result = service.createInternalMember(command)

        result.id shouldBe savedMember.id
        result.email shouldBe savedMember.email
        result.role shouldBe savedMember.role
        savedSlot.captured.email shouldBe command.email
        savedSlot.captured.password shouldBe "encoded-password"
        savedSlot.captured.provider shouldBe Provider.INTERNAL
        coVerify(exactly = 1) {
            transactionScope.registerEvent(
                match {
                    it is MemberCreateEvent &&
                        it.memberId == savedMember.id &&
                        it.email == savedMember.email
                }
            )
        }
    }

    @Test
    fun `소셜 회원가입 시 provider가 INTERNAL이면 예외가 발생한다`() = runTest {
        val transactional = mockk<Transactional>()
        val memberRepository = mockk<MemberRepository>()
        val emailVerificationCache = mockk<EmailVerificationCache>()
        val passwordEncoder = mockk<PasswordEncoder>()
        val socialMemberSessionCache = mockk<SocialMemberSessionCache>()
        val transactionScope = mockk<TransactionScope>(relaxed = true)
        val service = MemberService(
            transactional,
            memberRepository,
            emailVerificationCache,
            passwordEncoder,
            socialMemberSessionCache
        )
        val command = CreateSocialMemberCommand(
            email = "user@example.com",
            provider = Provider.INTERNAL,
            providerKey = "internal-key"
        )

        coEvery { transactional.invoke<MemberData>(any(), any()) } coAnswers {
            val block = secondArg<suspend TransactionScope.() -> MemberData>()
            block(transactionScope)
        }

        val exception = shouldThrow<IllegalStateException> {
            service.createSocialMember(command)
        }

        exception.message shouldBe "소셜 로그인으로 회원가입한 유저입니다."
        coVerify(exactly = 0) { memberRepository.save(any()) }
    }

    @Test
    fun `소셜 회원가입 시 이미 가입된 이메일이면 예외가 발생한다`() = runTest {
        val transactional = mockk<Transactional>()
        val memberRepository = mockk<MemberRepository>()
        val emailVerificationCache = mockk<EmailVerificationCache>()
        val passwordEncoder = mockk<PasswordEncoder>()
        val socialMemberSessionCache = mockk<SocialMemberSessionCache>()
        val transactionScope = mockk<TransactionScope>(relaxed = true)
        val service = MemberService(
            transactional,
            memberRepository,
            emailVerificationCache,
            passwordEncoder,
            socialMemberSessionCache
        )
        val command = CreateSocialMemberCommand(
            email = "user@example.com",
            provider = Provider.GITHUB,
            providerKey = "github-key"
        )

        coEvery { memberRepository.existsByEmail(command.email) } returns true
        coEvery { transactional.invoke<MemberData>(any(), any()) } coAnswers {
            val block = secondArg<suspend TransactionScope.() -> MemberData>()
            block(transactionScope)
        }

        val exception = shouldThrow<IllegalArgumentException> {
            service.createSocialMember(command)
        }

        exception.message shouldBe "이미 가입된 이메일 입니다."
        coVerify(exactly = 0) { memberRepository.save(any()) }
    }

    @Test
    fun `소셜 회원가입 시 회원을 저장하고 생성 이벤트를 등록한다`() = runTest {
        val transactional = mockk<Transactional>()
        val memberRepository = mockk<MemberRepository>()
        val emailVerificationCache = mockk<EmailVerificationCache>()
        val passwordEncoder = mockk<PasswordEncoder>()
        val socialMemberSessionCache = mockk<SocialMemberSessionCache>()
        val transactionScope = mockk<TransactionScope>(relaxed = true)
        val service = MemberService(
            transactional,
            memberRepository,
            emailVerificationCache,
            passwordEncoder,
            socialMemberSessionCache
        )
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
        val savedSlot = slot<server.feature.member.command.domain.Member>()

        coEvery { memberRepository.existsByEmail(command.email) } returns false
        coEvery { memberRepository.save(capture(savedSlot)) } returns savedMember
        coEvery { transactional.invoke<MemberData>(any(), any()) } coAnswers {
            val block = secondArg<suspend TransactionScope.() -> MemberData>()
            block(transactionScope)
        }

        val result = service.createSocialMember(command)

        result.id shouldBe savedMember.id
        result.email shouldBe savedMember.email
        result.role shouldBe savedMember.role
        savedSlot.captured.email shouldBe command.email
        savedSlot.captured.provider shouldBe command.provider
        savedSlot.captured.providerKey shouldBe command.providerKey
        coVerify(exactly = 1) {
            transactionScope.registerEvent(
                match {
                    it is MemberCreateEvent &&
                        it.memberId == savedMember.id &&
                        it.email == savedMember.email
                }
            )
        }
    }

    @Test
    fun `소셜 회원가입 세션 생성 시 토큰을 저장한다`() = runTest {
        val transactional = mockk<Transactional>()
        val memberRepository = mockk<MemberRepository>()
        val emailVerificationCache = mockk<EmailVerificationCache>()
        val passwordEncoder = mockk<PasswordEncoder>()
        val socialMemberSessionCache = mockk<SocialMemberSessionCache>()
        val transactionScope = mockk<TransactionScope>(relaxed = true)
        val service = MemberService(
            transactional,
            memberRepository,
            emailVerificationCache,
            passwordEncoder,
            socialMemberSessionCache
        )
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

        coEvery { memberRepository.existsByEmail(command.email) } returns false
        coEvery { memberRepository.save(any()) } returns savedMember
        coEvery { socialMemberSessionCache.set(any(), any()) } returns Unit
        coEvery { transactional.invoke<MemberData>(any(), any()) } coAnswers {
            val block = secondArg<suspend TransactionScope.() -> MemberData>()
            block(transactionScope)
        }

        val result = service.createSocialMemberWithSession(command)

        result.member.id shouldBe savedMember.id
        result.token.isNotBlank() shouldBe true
        coVerify(exactly = 1) { socialMemberSessionCache.set(result.token, savedMember.id) }
    }

    @Test
    fun `소셜 회원가입 세션 생성 시 회원가입 실패면 토큰을 저장하지 않는다`() = runTest {
        val transactional = mockk<Transactional>()
        val memberRepository = mockk<MemberRepository>()
        val emailVerificationCache = mockk<EmailVerificationCache>()
        val passwordEncoder = mockk<PasswordEncoder>()
        val socialMemberSessionCache = mockk<SocialMemberSessionCache>()
        val transactionScope = mockk<TransactionScope>(relaxed = true)
        val service = MemberService(
            transactional,
            memberRepository,
            emailVerificationCache,
            passwordEncoder,
            socialMemberSessionCache
        )
        val command = CreateSocialMemberCommand(
            email = "social@example.com",
            provider = Provider.GITHUB,
            providerKey = "github-key"
        )

        coEvery { memberRepository.existsByEmail(command.email) } returns true
        coEvery { transactional.invoke<MemberData>(any(), any()) } coAnswers {
            val block = secondArg<suspend TransactionScope.() -> MemberData>()
            block(transactionScope)
        }

        shouldThrow<IllegalArgumentException> {
            service.createSocialMemberWithSession(command)
        }

        coVerify(exactly = 0) { socialMemberSessionCache.set(any(), any()) }
    }

    @Test
    fun `회원 조회 시 존재하면 MemberData를 반환한다`() = runTest {
        val transactional = mockk<Transactional>()
        val memberRepository = mockk<MemberRepository>()
        val emailVerificationCache = mockk<EmailVerificationCache>()
        val passwordEncoder = mockk<PasswordEncoder>()
        val socialMemberSessionCache = mockk<SocialMemberSessionCache>()
        val service = MemberService(
            transactional,
            memberRepository,
            emailVerificationCache,
            passwordEncoder,
            socialMemberSessionCache
        )
        val memberId = 101L
        val member = createMember(id = memberId, email = "user@example.com")

        coEvery { memberRepository.findById(memberId) } returns member

        val result = service.findById(memberId)

        result?.id shouldBe memberId
        result?.email shouldBe member.email
        result?.role shouldBe member.role
    }

    @Test
    fun `회원 조회 시 존재하지 않으면 예외가 발생한다`() = runTest {
        val transactional = mockk<Transactional>()
        val memberRepository = mockk<MemberRepository>()
        val emailVerificationCache = mockk<EmailVerificationCache>()
        val passwordEncoder = mockk<PasswordEncoder>()
        val socialMemberSessionCache = mockk<SocialMemberSessionCache>()
        val service = MemberService(
            transactional,
            memberRepository,
            emailVerificationCache,
            passwordEncoder,
            socialMemberSessionCache
        )
        val memberId = 101L

        coEvery { memberRepository.findById(memberId) } returns null

        val exception = shouldThrow<IllegalArgumentException> {
            service.findById(memberId)
        }

        exception.message shouldBe "존재하지 않는 사용자 입니다."
    }

    @Test
    fun `소셜 회원 조회 시 존재하면 MemberData를 반환한다`() = runTest {
        val transactional = mockk<Transactional>()
        val memberRepository = mockk<MemberRepository>()
        val emailVerificationCache = mockk<EmailVerificationCache>()
        val passwordEncoder = mockk<PasswordEncoder>()
        val socialMemberSessionCache = mockk<SocialMemberSessionCache>()
        val service = MemberService(
            transactional,
            memberRepository,
            emailVerificationCache,
            passwordEncoder,
            socialMemberSessionCache
        )
        val member = createMember(
            id = 202L,
            email = "social@example.com",
            provider = Provider.GITHUB,
            providerKey = "github-key",
            password = ""
        )

        coEvery {
            memberRepository.findByProviderAndProviderKey(member.provider, member.providerKey)
        } returns member

        val result = service.findSocialMember(member.provider, member.providerKey)

        result.id shouldBe member.id
        result.email shouldBe member.email
        result.role shouldBe member.role
    }

    @Test
    fun `소셜 회원 조회 시 존재하지 않으면 예외가 발생한다`() = runTest {
        val transactional = mockk<Transactional>()
        val memberRepository = mockk<MemberRepository>()
        val emailVerificationCache = mockk<EmailVerificationCache>()
        val passwordEncoder = mockk<PasswordEncoder>()
        val socialMemberSessionCache = mockk<SocialMemberSessionCache>()
        val service = MemberService(
            transactional,
            memberRepository,
            emailVerificationCache,
            passwordEncoder,
            socialMemberSessionCache
        )

        coEvery {
            memberRepository.findByProviderAndProviderKey(Provider.GITHUB, "github-key")
        } returns null

        val exception = shouldThrow<IllegalArgumentException> {
            service.findSocialMember(Provider.GITHUB, "github-key")
        }

        exception.message shouldBe "존재하지 않는 사용자 입니다."
    }

    @Test
    fun `이메일 존재 여부 조회 시 exists가 true이면 true를 반환한다`() = runTest {
        val transactional = mockk<Transactional>()
        val memberRepository = mockk<MemberRepository>()
        val emailVerificationCache = mockk<EmailVerificationCache>()
        val passwordEncoder = mockk<PasswordEncoder>()
        val socialMemberSessionCache = mockk<SocialMemberSessionCache>()
        val service = MemberService(
            transactional,
            memberRepository,
            emailVerificationCache,
            passwordEncoder,
            socialMemberSessionCache
        )
        val command = EmailExistsCommand(email = "user@example.com")

        coEvery { memberRepository.existsByEmail(command.email) } returns true

        val result = service.emailExists(command)

        result.exists shouldBe true
    }

    @Test
    fun `이메일 존재 여부 조회 시 exists가 false이면 false를 반환한다`() = runTest {
        val transactional = mockk<Transactional>()
        val memberRepository = mockk<MemberRepository>()
        val emailVerificationCache = mockk<EmailVerificationCache>()
        val passwordEncoder = mockk<PasswordEncoder>()
        val socialMemberSessionCache = mockk<SocialMemberSessionCache>()
        val service = MemberService(
            transactional,
            memberRepository,
            emailVerificationCache,
            passwordEncoder,
            socialMemberSessionCache
        )
        val command = EmailExistsCommand(email = "user@example.com")

        coEvery { memberRepository.existsByEmail(command.email) } returns false

        val result = service.emailExists(command)

        result.exists shouldBe false
    }

    @Test
    fun `비밀번호 변경 시 동일한 비밀번호이면 예외가 발생한다`() = runTest {
        val transactional = mockk<Transactional>()
        val memberRepository = mockk<MemberRepository>()
        val emailVerificationCache = mockk<EmailVerificationCache>()
        val passwordEncoder = mockk<PasswordEncoder>()
        val socialMemberSessionCache = mockk<SocialMemberSessionCache>()
        val service = MemberService(
            transactional,
            memberRepository,
            emailVerificationCache,
            passwordEncoder,
            socialMemberSessionCache
        )
        val command = ChangePasswordCommand(
            oldPassword = "password!1",
            newPassword = "password!1",
            passwordConfirm = "password!1"
        )
        val passport = Passport(memberId = 1L, role = createMember().role)

        val exception = shouldThrow<IllegalArgumentException> {
            service.changePassword(command, passport)
        }

        exception.message shouldBe "같은 비밀번호는 사용할 수 없습니다."
    }

    @Test
    fun `비밀번호 변경 시 비밀번호 확인이 다르면 예외가 발생한다`() = runTest {
        val transactional = mockk<Transactional>()
        val memberRepository = mockk<MemberRepository>()
        val emailVerificationCache = mockk<EmailVerificationCache>()
        val passwordEncoder = mockk<PasswordEncoder>()
        val socialMemberSessionCache = mockk<SocialMemberSessionCache>()
        val service = MemberService(
            transactional,
            memberRepository,
            emailVerificationCache,
            passwordEncoder,
            socialMemberSessionCache
        )
        val command = ChangePasswordCommand(
            oldPassword = "password!1",
            newPassword = "password!2",
            passwordConfirm = "password!3"
        )
        val passport = Passport(memberId = 1L, role = createMember().role)

        val exception = shouldThrow<IllegalArgumentException> {
            service.changePassword(command, passport)
        }

        exception.message shouldBe "비밀번호가 일치하지 않습니다."
    }

    @Test
    fun `비밀번호 변경 시 사용자가 없으면 인증 예외가 발생한다`() = runTest {
        val transactional = mockk<Transactional>()
        val memberRepository = mockk<MemberRepository>()
        val emailVerificationCache = mockk<EmailVerificationCache>()
        val passwordEncoder = mockk<PasswordEncoder>()
        val socialMemberSessionCache = mockk<SocialMemberSessionCache>()
        val service = MemberService(
            transactional,
            memberRepository,
            emailVerificationCache,
            passwordEncoder,
            socialMemberSessionCache
        )
        val command = ChangePasswordCommand(
            oldPassword = "password!1",
            newPassword = "password!2",
            passwordConfirm = "password!2"
        )
        val passport = Passport(memberId = 404L, role = createMember().role)

        coEvery { memberRepository.findById(passport.memberId) } returns null

        shouldThrow<UnauthorizedException> {
            service.changePassword(command, passport)
        }
    }

    @Test
    fun `비밀번호 변경 시 내부 회원이 아니면 예외가 발생한다`() = runTest {
        val transactional = mockk<Transactional>()
        val memberRepository = mockk<MemberRepository>()
        val emailVerificationCache = mockk<EmailVerificationCache>()
        val passwordEncoder = mockk<PasswordEncoder>()
        val socialMemberSessionCache = mockk<SocialMemberSessionCache>()
        val service = MemberService(
            transactional,
            memberRepository,
            emailVerificationCache,
            passwordEncoder,
            socialMemberSessionCache
        )
        val command = ChangePasswordCommand(
            oldPassword = "password!1",
            newPassword = "password!2",
            passwordConfirm = "password!2"
        )
        val member = createMember(id = 1L, provider = Provider.GITHUB, providerKey = "github-key", password = "")
        val passport = Passport(memberId = member.id, role = member.role)

        coEvery { memberRepository.findById(passport.memberId) } returns member

        val exception = shouldThrow<IllegalArgumentException> {
            service.changePassword(command, passport)
        }

        exception.message shouldBe "이메일로 회원가입한 사용자가 아닙니다."
    }

    @Test
    fun `비밀번호 변경 시 기존 비밀번호가 다르면 예외가 발생한다`() = runTest {
        val transactional = mockk<Transactional>()
        val memberRepository = mockk<MemberRepository>()
        val emailVerificationCache = mockk<EmailVerificationCache>()
        val passwordEncoder = mockk<PasswordEncoder>()
        val socialMemberSessionCache = mockk<SocialMemberSessionCache>()
        val service = MemberService(
            transactional,
            memberRepository,
            emailVerificationCache,
            passwordEncoder,
            socialMemberSessionCache
        )
        val command = ChangePasswordCommand(
            oldPassword = "password!1",
            newPassword = "password!2",
            passwordConfirm = "password!2"
        )
        val member = createMember(id = 1L, password = "encoded-old")
        val passport = Passport(memberId = member.id, role = member.role)

        coEvery { memberRepository.findById(passport.memberId) } returns member
        every { passwordEncoder.matches(command.oldPassword, member.password) } returns false

        val exception = shouldThrow<IllegalArgumentException> {
            service.changePassword(command, passport)
        }

        exception.message shouldBe "기존 비밀번호가 일치하지 않습니다."
    }

    @Test
    fun `비밀번호 변경 시 새 비밀번호로 저장한다`() = runTest {
        val transactional = mockk<Transactional>()
        val memberRepository = mockk<MemberRepository>()
        val emailVerificationCache = mockk<EmailVerificationCache>()
        val passwordEncoder = mockk<PasswordEncoder>()
        val socialMemberSessionCache = mockk<SocialMemberSessionCache>()
        val service = MemberService(
            transactional,
            memberRepository,
            emailVerificationCache,
            passwordEncoder,
            socialMemberSessionCache
        )
        val command = ChangePasswordCommand(
            oldPassword = "password!1",
            newPassword = "password!2",
            passwordConfirm = "password!2"
        )
        val member = createMember(id = 1L, password = "encoded-old")
        val passport = Passport(memberId = member.id, role = member.role)
        val savedSlot = slot<server.feature.member.command.domain.Member>()

        coEvery { memberRepository.findById(passport.memberId) } returns member
        every { passwordEncoder.matches(command.oldPassword, member.password) } returns true
        every { passwordEncoder.encode(command.newPassword) } returns "encoded-new"
        coEvery { memberRepository.save(capture(savedSlot)) } returns member.copy(password = "encoded-new")

        val result = service.changePassword(command, passport)

        result.success shouldBe true
        savedSlot.captured.password shouldBe "encoded-new"
    }
}

package server.core.feature.member.application

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import server.core.feature.auth.infra.SocialMemberSessionCache
import server.core.feature.member.domain.Member
import server.core.feature.member.domain.MemberCreateEvent
import server.core.feature.member.domain.MemberRepository
import server.core.feature.member.domain.Provider
import server.core.global.security.Passport
import server.core.global.security.UnauthorizedException
import server.core.infra.event.TransactionalEventPublisher
import server.global.logging.biz
import server.password.PasswordEncoder
import java.util.*

@Service
@Transactional
class MemberService(
    private val memberRepository: MemberRepository,
    private val passwordEncoder: PasswordEncoder,
    private val socialMemberSessionCache: SocialMemberSessionCache,
    private val eventPublisher: TransactionalEventPublisher,
) {
    private val logger = KotlinLogging.logger {}

    fun createInternalMember(command: CreateInternalMemberCommand): MemberData {
        if (command.password != command.passwordConfirm) {
            throw IllegalArgumentException("비밀번호가 일치하지 않습니다.")
        }
        val encodedPassword = passwordEncoder.encode(command.password)

        val member = Member.fromInternal(
            email = command.email,
            password = encodedPassword
        )
        return createMember(member)
    }

    fun createSocialMember(command: CreateSocialMemberCommand): MemberData {
        val member = Member.fromSocial(
            email = command.email,
            provider = command.provider,
            providerKey = command.providerKey,
        )
        return createMember(member)
    }

    fun createSocialMemberWithSession(command: CreateSocialMemberCommand): CreateSocialMemberResult {
        val member = createMember(
            Member.Companion.fromSocial(
                email = command.email,
                provider = command.provider,
                providerKey = command.providerKey,
            )
        )
        val sessionToken = UUID.randomUUID().toString()
        socialMemberSessionCache.set(sessionToken, member.id)
        return CreateSocialMemberResult(member, sessionToken)
    }

    private fun createMember(member: Member): MemberData {
        if (memberRepository.existsByEmail(member.email)) {
            throw IllegalArgumentException("이미 가입된 이메일 입니다.")
        }
        val saved = memberRepository.save(member)

        eventPublisher.publish(
            MemberCreateEvent(
                memberId = saved.id,
                email = saved.email,
            )
        )
        logger.biz.info { "회원을 생성합니다" }

        return MemberData(saved)
    }

    @Transactional(readOnly = true)
    fun findById(memberId: Long): MemberData? {
        return memberRepository.findByIdOrNull(memberId)?.let(::MemberData)
            ?: throw IllegalArgumentException("존재하지 않는 사용자 입니다.")
    }

    @Transactional(readOnly = true)
    fun findSocialMember(provider: Provider, providerKey: String): MemberData {
        return memberRepository.findByProviderAndProviderKey(
            provider = provider,
            providerKey = providerKey,
        )
            ?.let(::MemberData)
            ?: throw IllegalArgumentException("존재하지 않는 사용자 입니다.")
    }

    @Transactional(readOnly = true)
    fun emailExists(command: EmailExistsCommand): EmailExistsResult {
        val exists = memberRepository.existsByEmail(command.email)
        return EmailExistsResult(exists)
    }

    fun changePassword(
        command: ChangePasswordCommand,
        passport: Passport
    ): ChangePasswordResult {
        logger.biz.info { "비밀번호를 변경합니다" }
        if (command.oldPassword == command.newPassword) {
            throw IllegalArgumentException("같은 비밀번호는 사용할 수 없습니다.")
        }

        if (command.newPassword != command.passwordConfirm) {
            throw IllegalArgumentException("비밀번호가 일치하지 않습니다.")
        }
        val member = memberRepository.findByIdOrNull(passport.memberId)
            ?: throw UnauthorizedException()
        if (member.provider != Provider.INTERNAL) {
            throw IllegalArgumentException("이메일로 회원가입한 사용자가 아닙니다.")
        }
        if (!passwordEncoder.matches(command.oldPassword, member.password)) {
            throw IllegalArgumentException("기존 비밀번호가 일치하지 않습니다.")
        }

        member.updatePassword(passwordEncoder.encode(command.newPassword))

        return ChangePasswordResult(true)
    }
}

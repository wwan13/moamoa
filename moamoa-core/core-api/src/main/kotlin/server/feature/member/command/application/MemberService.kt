package server.feature.member.command.application

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import server.feature.member.command.domain.Member
import server.feature.member.command.domain.MemberRepository
import server.feature.member.command.domain.Provider
import server.global.logging.event
import server.infra.cache.EmailVerificationCache
import server.infra.cache.SocialMemberSessionCache
import server.infra.db.transaction.Transactional
import server.shared.security.password.PasswordEncoder
import server.security.Passport
import server.security.UnauthorizedException
import java.util.*

@Service
class MemberService(
    private val transactional: Transactional,
    private val memberRepository: MemberRepository,
    private val emailVerificationCache: EmailVerificationCache,
    private val passwordEncoder: PasswordEncoder,
    private val socialMemberSessionCache: SocialMemberSessionCache
) {
    private val logger = KotlinLogging.logger {}

    fun createInternalMember(command: CreateInternalMemberCommand): MemberData {
//        if (!emailVerificationCache.isVerified(command.email)) {
//            throw IllegalArgumentException("인증되지 않은 이메일 입니다.")
//        }

        if (command.password != command.passwordConfirm) {
            throw IllegalArgumentException("비밀번호가 일치하지 않습니다.")
        }
        val encodedPassword = passwordEncoder.encode(command.password)

        return transactional {
            val member = Member.fromInternal(
                email = command.email,
                password = encodedPassword
            )
            createMember(member)
        }
    }

    fun createSocialMember(command: CreateSocialMemberCommand): MemberData {
        val member = Member.fromSocial(
            email = command.email,
            provider = command.provider,
            providerKey = command.providerKey,
        )
        return transactional {
            createMember(member)
        }
    }

    fun createSocialMemberWithSession(command: CreateSocialMemberCommand): CreateSocialMemberResult {
        val member = transactional {
            createMember(
                Member.fromSocial(
                    email = command.email,
                    provider = command.provider,
                    providerKey = command.providerKey,
                )
            )
        }
        val sessionToken = UUID.randomUUID().toString()
        socialMemberSessionCache.set(sessionToken, member.id)
        return CreateSocialMemberResult(member, sessionToken)
    }

    private fun createMember(member: Member): MemberData = transactional {
        if (memberRepository.existsByEmail(member.email)) {
            throw IllegalArgumentException("이미 가입된 이메일 입니다.")
        }
        val saved = memberRepository.save(member)

        val event = saved.created()
        registerEvent(event)
        logger.event.info(event) {
            "회원 생성 이벤트를 발행했습니다"
        }

        MemberData(saved)
    }

    fun findById(memberId: Long): MemberData? {
        return memberRepository.findByIdOrNull(memberId)?.let(::MemberData)
            ?: throw IllegalArgumentException("존재하지 않는 사용자 입니다.")
    }

    fun findSocialMember(provider: Provider, providerKey: String): MemberData {
        return memberRepository.findByProviderAndProviderKey(
            provider = provider,
            providerKey = providerKey,
        )
            ?.let(::MemberData)
            ?: throw IllegalArgumentException("존재하지 않는 사용자 입니다.")
    }

    fun emailExists(command: EmailExistsCommand): EmailExistsResult {
        val exists = memberRepository.existsByEmail(command.email)
        return EmailExistsResult(exists)
    }

    fun changePassword(
        command: ChangePasswordCommand,
        passport: Passport
    ): ChangePasswordResult {
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
        memberRepository.save(member)

        return ChangePasswordResult(true)
    }
}

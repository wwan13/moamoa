package server.feature.member.application

import org.springframework.stereotype.Service
import server.feature.member.domain.Member
import server.feature.member.domain.MemberRepository
import server.feature.member.domain.Provider
import server.infra.cache.EmailVerificationCache
import server.infra.db.Transactional
import server.password.PasswordEncoder

@Service
class MemberService(
    private val transactional: Transactional,
    private val memberRepository: MemberRepository,
    private val emailVerificationCache: EmailVerificationCache,
    private val passwordEncoder: PasswordEncoder,
) {

    suspend fun createInternalMember(command: CreateInternalMemberCommand): MemberData {
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

    suspend fun createSocialMember(command: CreateSocialMemberCommand): MemberData = transactional {
        val member = Member.fromSocial(
            email = command.email,
            provider = command.provider,
            providerKey = command.providerKey,
        )
        createMember(member)
    }

    private suspend fun createMember(member: Member): MemberData {
        if (memberRepository.existsByEmail(member.email)) {
            throw IllegalArgumentException("이미 가입된 이메일 입니다.")
        }
        return memberRepository.save(member).let(::MemberData)
    }

    suspend fun findById(memberId: Long): MemberData? {
        return memberRepository.findById(memberId)?.let(::MemberData)
            ?: throw IllegalArgumentException("존재하지 않는 사용자 입니다.")
    }

    suspend fun findSocialMember(provider: Provider, providerKey: String): MemberData {
        return memberRepository.findByProviderAndProviderKey(
            provider = provider,
            providerKey = providerKey,
        )
            ?.let(::MemberData)
            ?: throw IllegalArgumentException("존재하지 않는 사용자 입니다.")
    }

    suspend fun emailExists(command: EmailExistsCommand): EmailExistsResult {
        val exists = memberRepository.existsByEmail(command.email)
        return EmailExistsResult(exists)
    }
}
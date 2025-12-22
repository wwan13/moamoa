package server.application

import org.springframework.stereotype.Service
import server.application.cache.EmailVerificationCache
import server.domain.member.Member
import server.domain.member.MemberRepository
import server.domain.member.MemberRole
import server.infra.security.password.PasswordEncoder

@Service
class MemberService(
    private val transactional: Transactional,
    private val memberRepository: MemberRepository,
    private val emailVerificationCache: EmailVerificationCache,
    private val passwordEncoder: PasswordEncoder,
) {

    suspend fun createMember(command: CreateMemberCommand): MemberData {
        if (!emailVerificationCache.isVerified(command.email)) {
            throw IllegalArgumentException("인증되지 않은 이메일 입니다.")
        }
        if (command.password != command.passwordConfirm) {
            throw IllegalArgumentException("비밀번호 확인이 올반르지 않습니다.")
        }
        val encodedPassword = passwordEncoder.encode(command.password)

        return transactional {
            if (memberRepository.existsByEmail(command.email)) {
                throw IllegalArgumentException("이미 존재하는 이메일 입니다.")
            }

            val member = Member(
                role = MemberRole.USER,
                email = command.email,
                password = encodedPassword
            )
            memberRepository.save(member).let(::MemberData)
        }
    }

    suspend fun emailExists(email: String): Boolean {
        return memberRepository.existsByEmail(email)
    }
}
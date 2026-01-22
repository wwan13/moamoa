package server.feature.member.command.application

import org.springframework.stereotype.Service
import server.feature.member.command.domain.MemberRepository
import server.feature.postbookmark.domain.PostBookmarkRepository
import server.feature.submission.domain.SubmissionRepository
import server.feature.techblogsubscription.domain.TechBlogSubscriptionRepository
import server.infra.db.Transactional
import server.security.Passport

@Service
class MemberUnjoinService(
    private val transactional: Transactional,
    private val memberRepository: MemberRepository,
    private val postBookmarkRepository: PostBookmarkRepository,
    private val techBlogSubscriptionRepository: TechBlogSubscriptionRepository,
    private val submissionRepository: SubmissionRepository
) {

    suspend fun unjoin(passport: Passport) = transactional {
        postBookmarkRepository.deleteAllByMemberId(passport.memberId)
        techBlogSubscriptionRepository.deleteAllByMemberId(passport.memberId)
        submissionRepository.deleteAllByMemberId(passport.memberId)
        memberRepository.deleteById(passport.memberId)

        MemberUnjoinResult(true)
    }
}
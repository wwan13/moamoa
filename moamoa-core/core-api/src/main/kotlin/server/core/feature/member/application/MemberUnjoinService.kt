package server.core.feature.member.application

import org.springframework.stereotype.Service
import server.core.feature.member.domain.MemberRepository
import server.core.feature.postbookmark.domain.PostBookmarkRepository
import server.core.feature.submission.domain.SubmissionRepository
import server.core.feature.techblogsubscription.domain.TechBlogSubscriptionRepository
import server.core.infra.db.transaction.Transactional
import server.core.global.security.Passport

@Service
class MemberUnjoinService(
    private val transactional: Transactional,
    private val memberRepository: MemberRepository,
    private val postBookmarkRepository: PostBookmarkRepository,
    private val techBlogSubscriptionRepository: TechBlogSubscriptionRepository,
    private val submissionRepository: SubmissionRepository
) {

    fun unjoin(passport: Passport) = transactional {
        postBookmarkRepository.deleteAllByMemberId(passport.memberId)
        techBlogSubscriptionRepository.deleteAllByMemberId(passport.memberId)
        submissionRepository.deleteAllByMemberId(passport.memberId)
        memberRepository.deleteById(passport.memberId)

        MemberUnjoinResult(true)
    }
}

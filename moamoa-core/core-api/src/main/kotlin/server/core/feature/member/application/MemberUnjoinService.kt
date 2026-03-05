package server.core.feature.member.application

import org.springframework.stereotype.Service
import server.core.feature.member.domain.MemberRepository
import server.core.feature.bookmark.domain.BookmarkRepository
import server.core.feature.submission.domain.SubmissionRepository
import server.core.feature.subscription.domain.SubscriptionRepository
import server.core.infra.db.transaction.Transactional
import server.core.global.security.Passport

@Service
class MemberUnjoinService(
    private val transactional: Transactional,
    private val memberRepository: MemberRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val submissionRepository: SubmissionRepository
) {

    fun unjoin(passport: Passport) = transactional {
        bookmarkRepository.deleteAllByMemberId(passport.memberId)
        subscriptionRepository.deleteAllByMemberId(passport.memberId)
        submissionRepository.deleteAllByMemberId(passport.memberId)
        memberRepository.deleteById(passport.memberId)

        MemberUnjoinResult(true)
    }
}

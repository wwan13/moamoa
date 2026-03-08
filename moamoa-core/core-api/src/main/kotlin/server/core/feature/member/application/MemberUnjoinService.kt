package server.core.feature.member.application

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import server.core.feature.member.domain.MemberRepository
import server.core.feature.bookmark.domain.BookmarkRepository
import server.core.feature.submission.domain.SubmissionRepository
import server.core.feature.subscription.domain.SubscriptionRepository
import server.core.infra.db.transaction.Transactional
import server.core.global.security.Passport
import server.global.logging.biz

@Service
class MemberUnjoinService(
    private val transactional: Transactional,
    private val memberRepository: MemberRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val submissionRepository: SubmissionRepository
) {
    private val logger = KotlinLogging.logger {}

    fun unjoin(passport: Passport) = transactional {
        logger.biz.info { "회원 탈퇴를 처리합니다" }
        bookmarkRepository.deleteAllByMemberId(passport.memberId)
        subscriptionRepository.deleteAllByMemberId(passport.memberId)
        submissionRepository.deleteAllByMemberId(passport.memberId)
        memberRepository.deleteById(passport.memberId)

        MemberUnjoinResult(true)
    }
}

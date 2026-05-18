package server.core.feature.member.application

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import server.core.feature.auth.infra.RefreshTokenCache
import server.core.feature.member.domain.MemberRepository
import server.core.feature.member.infra.MemberEventPublisher
import server.core.feature.bookmark.domain.BookmarkRepository
import server.core.feature.submission.domain.SubmissionRepository
import server.core.feature.subscription.domain.SubscriptionRepository
import server.core.global.security.Passport
import server.global.logging.biz

@Service
@Transactional
class MemberUnjoinService(
    private val memberRepository: MemberRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val submissionRepository: SubmissionRepository,
    private val refreshTokenCache: RefreshTokenCache,
    private val memberEventPublisher: MemberEventPublisher,
) {
    private val logger = KotlinLogging.logger {}

    fun unjoin(passport: Passport) {
        logger.biz.info { "회원 탈퇴를 처리합니다" }
        bookmarkRepository.deleteAllByMemberId(passport.memberId)
        subscriptionRepository.deleteAllByMemberId(passport.memberId)
        submissionRepository.deleteAllByMemberId(passport.memberId)
        memberRepository.deleteById(passport.memberId)
        refreshTokenCache.evict(passport.memberId)
        memberEventPublisher.publishUnjoined(passport.memberId)
    }
}

package server.core.feature.member.application

import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import server.core.feature.auth.infra.RefreshTokenCache
import server.core.feature.bookmark.domain.BookmarkRepository
import server.core.feature.member.domain.MemberRepository
import server.core.feature.member.infra.MemberEventPublisher
import server.core.feature.submission.domain.SubmissionRepository
import server.core.feature.subscription.domain.SubscriptionRepository
import server.core.global.security.Passport
import server.core.feature.member.domain.MemberRole
import test.UnitTest

class MemberUnjoinServiceTest : UnitTest() {
    @Test
    fun `회원 탈퇴 시 연관 데이터 삭제와 refresh token 제거 후 탈퇴 이벤트를 발행한다`() {
        val memberRepository = mockk<MemberRepository>(relaxed = true)
        val bookmarkRepository = mockk<BookmarkRepository>(relaxed = true)
        val subscriptionRepository = mockk<SubscriptionRepository>(relaxed = true)
        val submissionRepository = mockk<SubmissionRepository>(relaxed = true)
        val refreshTokenCache = mockk<RefreshTokenCache>(relaxed = true)
        val memberEventPublisher = mockk<MemberEventPublisher>(relaxed = true)
        val service = MemberUnjoinService(
            memberRepository = memberRepository,
            bookmarkRepository = bookmarkRepository,
            subscriptionRepository = subscriptionRepository,
            submissionRepository = submissionRepository,
            refreshTokenCache = refreshTokenCache,
            memberEventPublisher = memberEventPublisher,
        )

        service.unjoin(Passport(memberId = 12L, role = MemberRole.USER))

        verify(exactly = 1) { bookmarkRepository.deleteAllByMemberId(12L) }
        verify(exactly = 1) { subscriptionRepository.deleteAllByMemberId(12L) }
        verify(exactly = 1) { submissionRepository.deleteAllByMemberId(12L) }
        verify(exactly = 1) { memberRepository.deleteById(12L) }
        verify(exactly = 1) { refreshTokenCache.evict(12L) }
        verify(exactly = 1) { memberEventPublisher.publishUnjoined(12L) }
    }
}

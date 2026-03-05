package server.core.feature.member.query

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import server.core.feature.member.domain.MemberRepository
import server.core.feature.bookmark.domain.BookmarkRepository
import server.core.feature.subscription.domain.SubscriptionRepository
import server.core.infra.cache.BookmarkedAllPostIdSetCache
import server.core.infra.cache.SubscriptionCache

@Service
class MemberQueryService(
    private val memberRepository: MemberRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val subscriptionCache: SubscriptionCache,
    private val bookmarkedAllPostIdSetCache: BookmarkedAllPostIdSetCache
) {

    fun findById(memberId: Long): MemberSummary {
        val member = memberRepository.findByIdOrNull(memberId)
            ?: throw IllegalArgumentException("존재하지 않는 사용자 입니다.")

        val subscriptionCount = subscriptionCache.get(memberId)?.count()?.toLong()
            ?: subscriptionRepository.countByMemberId(memberId)
        val bookmarkCount = bookmarkedAllPostIdSetCache.get(memberId)?.count()?.toLong()
            ?: bookmarkRepository.countByMemberId(memberId)

        return MemberSummary(
            memberId = member.id,
            email = member.email,
            provider = member.provider,
            subscribeCount = subscriptionCount,
            bookmarkCount = bookmarkCount,
        )
    }
}

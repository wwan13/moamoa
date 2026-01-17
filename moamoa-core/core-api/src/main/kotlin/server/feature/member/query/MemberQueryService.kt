package server.feature.member.query

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Service
import server.feature.member.command.domain.MemberRepository
import server.feature.postbookmark.domain.PostBookmarkRepository
import server.feature.techblogsubscription.domain.TechBlogSubscriptionRepository
import server.infra.cache.BookmarkedAllPostIdSetCache
import server.infra.cache.TechBlogSubscriptionCache

@Service
class MemberQueryService(
    private val memberRepository: MemberRepository,
    private val techBlogSubscriptionRepository: TechBlogSubscriptionRepository,
    private val postBookmarkRepository: PostBookmarkRepository,
    private val techBlogSubscriptionCache: TechBlogSubscriptionCache,
    private val bookmarkedAllPostIdSetCache: BookmarkedAllPostIdSetCache
) {

    suspend fun findById(memberId: Long): MemberSummary = coroutineScope {
        val member = memberRepository.findById(memberId)
            ?: throw IllegalArgumentException("존재하지 않는 사용자 입니다.")

        val subscriptionCountDeferred = async {
            techBlogSubscriptionCache.get(memberId)?.count()?.toLong()
                ?: techBlogSubscriptionRepository.countByMemberId(memberId)
        }
        val bookmarkCountDeferred = async {
            bookmarkedAllPostIdSetCache.get(memberId)?.count()?.toLong()
                ?: postBookmarkRepository.countByMemberId(memberId)
        }

        val subscriptionCount = subscriptionCountDeferred.await()
        val bookmarkCount = bookmarkCountDeferred.await()

        MemberSummary(
            memberId = member.id,
            email = member.email,
            provider = member.provider,
            subscribeCount = subscriptionCount,
            bookmarkCount = bookmarkCount,
        )
    }
}
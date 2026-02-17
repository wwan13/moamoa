package server.feature.post.command.application

import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import server.feature.postbookmark.domain.PostBookmarkUpdatedEvent
import server.feature.techblogsubscription.domain.TechBlogSubscribeUpdatedEvent
import server.infra.cache.BookmarkedAllPostIdSetCache
import server.infra.cache.BookmarkedPostListCache
import server.infra.cache.PostStatsCache
import server.infra.cache.SubscribedPostListCache
import server.shared.messaging.SubscriptionDefinition
import test.UnitTest

class PostCacheEvictServiceTest : UnitTest() {
    @Test
    fun `구독 정보가 변경되면 구독 목록 캐시를 무효화한다`() = runTest {
        val postCacheHandlingStream = mockk<SubscriptionDefinition>()
        val subscribedPostListCache = mockk<SubscribedPostListCache>(relaxed = true)
        val bookmarkedPostListCache = mockk<BookmarkedPostListCache>(relaxed = true)
        val postStatsCache = mockk<PostStatsCache>(relaxed = true)
        val bookmarkedAllPostIdSetCache = mockk<BookmarkedAllPostIdSetCache>(relaxed = true)
        val service = PostCacheEvictService(
            postCacheHandlingStream,
            subscribedPostListCache,
            bookmarkedPostListCache,
            postStatsCache,
            bookmarkedAllPostIdSetCache
        )
        val event = TechBlogSubscribeUpdatedEvent(memberId = 10L, techBlogId = 20L, subscribed = true)

        val handler = service.subscriptionUpdatedPostCacheEvict()
        handler.handler(event)

        coVerify(exactly = 1) { subscribedPostListCache.evictAll(event.memberId) }
    }

    @Test
    fun `북마크 정보가 변경되면 관련 캐시를 무효화한다`() = runTest {
        val postCacheHandlingStream = mockk<SubscriptionDefinition>()
        val subscribedPostListCache = mockk<SubscribedPostListCache>(relaxed = true)
        val bookmarkedPostListCache = mockk<BookmarkedPostListCache>(relaxed = true)
        val postStatsCache = mockk<PostStatsCache>(relaxed = true)
        val bookmarkedAllPostIdSetCache = mockk<BookmarkedAllPostIdSetCache>(relaxed = true)
        val service = PostCacheEvictService(
            postCacheHandlingStream,
            subscribedPostListCache,
            bookmarkedPostListCache,
            postStatsCache,
            bookmarkedAllPostIdSetCache
        )
        val event = PostBookmarkUpdatedEvent(memberId = 10L, postId = 200L, bookmarked = true)

        val handler = service.bookmarkUpdatedPostCacheEvict()
        handler.handler(event)

        coVerify(exactly = 1) { bookmarkedPostListCache.evictAll(event.memberId) }
        coVerify(exactly = 1) { postStatsCache.evict(event.postId) }
        coVerify(exactly = 1) { bookmarkedAllPostIdSetCache.evictAll(event.memberId) }
    }
}

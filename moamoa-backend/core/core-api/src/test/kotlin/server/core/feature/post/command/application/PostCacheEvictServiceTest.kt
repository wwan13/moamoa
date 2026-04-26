package server.core.feature.post.command.application

import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import server.core.feature.post.application.PostCacheEvictService
import server.core.feature.bookmark.application.BookmarkUpdatedEvent
import server.core.feature.subscription.application.TechBlogSubscribeUpdatedEvent
import server.core.feature.post.infra.BookmarkedAllPostIdSetCache
import server.core.feature.post.infra.BookmarkedPostListCache
import server.core.feature.post.infra.PostStatsCache
import server.core.feature.post.infra.SubscribedPostListCache
import test.UnitTest

class PostCacheEvictServiceTest : UnitTest() {
    @Test
    fun `구독 정보가 변경되면 구독 목록 캐시를 무효화한다`() = runTest {
        val subscribedPostListCache = mockk<SubscribedPostListCache>(relaxed = true)
        val bookmarkedPostListCache = mockk<BookmarkedPostListCache>(relaxed = true)
        val postStatsCache = mockk<PostStatsCache>(relaxed = true)
        val bookmarkedAllPostIdSetCache = mockk<BookmarkedAllPostIdSetCache>(relaxed = true)
        val service = PostCacheEvictService(
            subscribedPostListCache,
            bookmarkedPostListCache,
            postStatsCache,
            bookmarkedAllPostIdSetCache
        )
        val event = TechBlogSubscribeUpdatedEvent(memberId = 10L, techBlogId = 20L, subscribed = true)

        service.subscriptionUpdatedPostCacheEvict(event)

        coVerify(exactly = 1) { subscribedPostListCache.evictAll(event.memberId) }
    }

    @Test
    fun `북마크 정보가 변경되면 관련 캐시를 무효화한다`() = runTest {
        val subscribedPostListCache = mockk<SubscribedPostListCache>(relaxed = true)
        val bookmarkedPostListCache = mockk<BookmarkedPostListCache>(relaxed = true)
        val postStatsCache = mockk<PostStatsCache>(relaxed = true)
        val bookmarkedAllPostIdSetCache = mockk<BookmarkedAllPostIdSetCache>(relaxed = true)
        val service = PostCacheEvictService(
            subscribedPostListCache,
            bookmarkedPostListCache,
            postStatsCache,
            bookmarkedAllPostIdSetCache
        )
        val event = BookmarkUpdatedEvent(memberId = 10L, postId = 200L, bookmarked = true)

        service.bookmarkUpdatedPostCacheEvict(event)

        coVerify(exactly = 1) { bookmarkedPostListCache.evictAll(event.memberId) }
        coVerify(exactly = 1) { postStatsCache.evict(event.postId) }
        coVerify(exactly = 1) { bookmarkedAllPostIdSetCache.evictAll(event.memberId) }
    }
}

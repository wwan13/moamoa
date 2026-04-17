package server.core.feature.post.application

import org.springframework.stereotype.Service
import server.core.feature.bookmark.domain.BookmarkUpdatedEvent
import server.core.feature.post.infra.BookmarkedAllPostIdSetCache
import server.core.feature.post.infra.BookmarkedPostListCache
import server.core.feature.post.infra.PostStatsCache
import server.core.feature.post.infra.SubscribedPostListCache
import server.core.feature.subscription.domain.TechBlogSubscribeUpdatedEvent
import server.messaging.annotation.EventHandler
import server.messaging.definition.EventStream

@Service
class PostCacheEvictService(
    private val subscribedPostListCache: SubscribedPostListCache,
    private val bookmarkedPostListCache: BookmarkedPostListCache,
    private val postStatsCache: PostStatsCache,
    private val bookmarkedAllPostIdSetCache: BookmarkedAllPostIdSetCache
) {
    @EventHandler(EventStream.POST_CACHE_HANDLING)
    fun subscriptionUpdatedPostCacheEvict(event: TechBlogSubscribeUpdatedEvent) {
        subscribedPostListCache.evictAll(event.memberId)
    }

    @EventHandler(EventStream.POST_CACHE_HANDLING)
    fun bookmarkUpdatedPostCacheEvict(event: BookmarkUpdatedEvent) {
        bookmarkedPostListCache.evictAll(event.memberId)
        postStatsCache.evict(event.postId)
        bookmarkedAllPostIdSetCache.evictAll(event.memberId)
    }
}

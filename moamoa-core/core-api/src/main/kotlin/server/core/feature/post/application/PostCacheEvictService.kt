package server.core.feature.post.application

import org.springframework.stereotype.Service
import server.core.feature.bookmark.domain.BookmarkUpdatedEvent
import server.core.feature.post.infra.BookmarkedAllPostIdSetCache
import server.core.feature.post.infra.BookmarkedPostListCache
import server.core.feature.post.infra.PostStatsCache
import server.core.feature.post.infra.SubscribedPostListCache
import server.core.feature.subscription.domain.TechBlogSubscribeUpdatedEvent
import server.messaging.EventHandler
import server.messaging.SubscriptionDefinition
import server.messaging.invoke

@Service
class PostCacheEvictService(
    private val postCacheHandlingStream: SubscriptionDefinition,
    private val eventHandler: EventHandler,
    private val subscribedPostListCache: SubscribedPostListCache,
    private val bookmarkedPostListCache: BookmarkedPostListCache,
    private val postStatsCache: PostStatsCache,
    private val bookmarkedAllPostIdSetCache: BookmarkedAllPostIdSetCache
) {
    fun subscriptionUpdatedPostCacheEvict() =
        eventHandler<TechBlogSubscribeUpdatedEvent>(postCacheHandlingStream) { event ->
            subscribedPostListCache.evictAll(event.memberId)
        }

    fun bookmarkUpdatedPostCacheEvict() =
        eventHandler<BookmarkUpdatedEvent>(postCacheHandlingStream) { event ->
            bookmarkedPostListCache.evictAll(event.memberId)
            postStatsCache.evict(event.postId)
            bookmarkedAllPostIdSetCache.evictAll(event.memberId)
        }
}

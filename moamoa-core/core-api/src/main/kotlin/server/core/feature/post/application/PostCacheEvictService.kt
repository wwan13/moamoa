package server.core.feature.post.application

import org.springframework.stereotype.Service
import server.core.feature.bookmark.domain.BookmarkUpdatedEvent
import server.core.feature.subscription.domain.TechBlogSubscribeUpdatedEvent
import server.core.feature.post.infra.BookmarkedAllPostIdSetCache
import server.core.feature.post.infra.BookmarkedPostListCache
import server.core.feature.post.infra.PostStatsCache
import server.core.feature.post.infra.SubscribedPostListCache
import server.messaging.EventHandler
import server.messaging.SubscriptionDefinition

@Service
class PostCacheEvictService(
    private val postCacheHandlingStream: SubscriptionDefinition,
    private val subscribedPostListCache: SubscribedPostListCache,
    private val bookmarkedPostListCache: BookmarkedPostListCache,
    private val postStatsCache: PostStatsCache,
    private val bookmarkedAllPostIdSetCache: BookmarkedAllPostIdSetCache
) {

    @EventHandler
    fun subscriptionUpdatedPostCacheEvict() =
        _root_ide_package_.server.messaging.handleMessage<TechBlogSubscribeUpdatedEvent>(postCacheHandlingStream) { event ->
            subscribedPostListCache.evictAll(event.memberId)
        }

    @EventHandler
    fun bookmarkUpdatedPostCacheEvict() =
        _root_ide_package_.server.messaging.handleMessage<BookmarkUpdatedEvent>(postCacheHandlingStream) { event ->
            bookmarkedPostListCache.evictAll(event.memberId)
            postStatsCache.evict(event.postId)
            bookmarkedAllPostIdSetCache.evictAll(event.memberId)
        }
}
package server.core.feature.post.application

import org.springframework.stereotype.Service
import server.core.feature.postbookmark.domain.PostBookmarkUpdatedEvent
import server.core.feature.techblogsubscription.domain.TechBlogSubscribeUpdatedEvent
import server.core.infra.cache.BookmarkedAllPostIdSetCache
import server.core.infra.cache.BookmarkedPostListCache
import server.core.infra.cache.PostStatsCache
import server.core.infra.cache.SubscribedPostListCache
import server.messaging.EventHandler
import server.messaging.SubscriptionDefinition
import server.messaging.handleMessage

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
        _root_ide_package_.server.messaging.handleMessage<PostBookmarkUpdatedEvent>(postCacheHandlingStream) { event ->
            bookmarkedPostListCache.evictAll(event.memberId)
            postStatsCache.evict(event.postId)
            bookmarkedAllPostIdSetCache.evictAll(event.memberId)
        }
}
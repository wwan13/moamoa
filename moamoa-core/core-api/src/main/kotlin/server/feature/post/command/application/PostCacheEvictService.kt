package server.feature.post.command.application

import org.springframework.stereotype.Service
import server.feature.postbookmark.domain.PostBookmarkUpdatedEvent
import server.feature.techblogsubscription.domain.TechBlogSubscribeUpdatedEvent
import server.infra.cache.BookmarkedAllPostIdSetCache
import server.infra.cache.BookmarkedPostListCache
import server.infra.cache.PostStatsCache
import server.infra.cache.SubscribedPostListCache
import server.shared.messaging.EventHandler
import server.shared.messaging.SubscriptionDefinition
import server.shared.messaging.handleMessage

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
        handleMessage<TechBlogSubscribeUpdatedEvent>(postCacheHandlingStream) { event ->
            subscribedPostListCache.evictAll(event.memberId)
        }

    @EventHandler
    fun bookmarkUpdatedPostCacheEvict() =
        handleMessage<PostBookmarkUpdatedEvent>(postCacheHandlingStream) { event ->
            bookmarkedPostListCache.evictAll(event.memberId)
            postStatsCache.evict(event.postId)
            bookmarkedAllPostIdSetCache.evictAll(event.memberId)
        }
}
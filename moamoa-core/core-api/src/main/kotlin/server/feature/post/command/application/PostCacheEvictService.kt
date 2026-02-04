package server.feature.post.command.application

import org.springframework.stereotype.Service
import server.feature.postbookmark.domain.PostBookmarkUpdatedEvent
import server.feature.techblogsubscription.domain.TechBlogSubscribeUpdatedEvent
import server.infra.cache.BookmarkedAllPostIdSetCache
import server.infra.cache.BookmarkedPostListCache
import server.infra.cache.PostStatsCache
import server.infra.cache.SubscribedPostListCache
import server.messaging.EventHandler
import server.messaging.StreamDefinition
import server.messaging.handleEvent

@Service
class PostCacheEvictService(
    private val postCacheHandlingStream: StreamDefinition,
    private val subscribedPostListCache: SubscribedPostListCache,
    private val bookmarkedPostListCache: BookmarkedPostListCache,
    private val postStatsCache: PostStatsCache,
    private val bookmarkedAllPostIdSetCache: BookmarkedAllPostIdSetCache
) {

    @EventHandler
    fun subscriptionUpdatedPostCacheEvict() =
        handleEvent<TechBlogSubscribeUpdatedEvent>(postCacheHandlingStream) { event ->
            subscribedPostListCache.evictAll(event.memberId)
        }

    @EventHandler
    fun bookmarkUpdatedPostCacheEvict() =
        handleEvent<PostBookmarkUpdatedEvent>(postCacheHandlingStream) { event ->
            bookmarkedPostListCache.evictAll(event.memberId)
            postStatsCache.evict(event.postId)
            bookmarkedAllPostIdSetCache.evictAll(event.memberId)
        }
}
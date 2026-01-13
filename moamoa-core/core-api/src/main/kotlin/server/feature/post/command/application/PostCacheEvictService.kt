package server.feature.post.command.application

import org.springframework.stereotype.Service
import server.feature.postbookmark.domain.PostBookmarkCreatedEvent
import server.feature.postbookmark.domain.PostBookmarkRemovedEvent
import server.feature.techblog.command.domain.TechBlogSubscribeCreatedEvent
import server.feature.techblog.command.domain.TechBlogSubscribeRemovedEvent
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
    fun subscriptionCreatedPostCacheEvict() =
        handleEvent<TechBlogSubscribeCreatedEvent>(postCacheHandlingStream) { event ->
            subscribedPostListCache.evictAll(event.memberId)
        }

    @EventHandler
    fun subscriptionRemovedPostCacheEvict() =
        handleEvent<TechBlogSubscribeRemovedEvent>(postCacheHandlingStream) { event ->
            subscribedPostListCache.evictAll(event.memberId)
        }

    @EventHandler
    fun bookmarkCreatedPostCacheEvict() =
        handleEvent<PostBookmarkCreatedEvent>(postCacheHandlingStream) { event ->
            bookmarkedPostListCache.evictAll(event.memberId)
            postStatsCache.evict(event.postId)
            bookmarkedAllPostIdSetCache.evictAll(event.postId)
        }

    @EventHandler
    fun bookmarkRemovedPostCacheEvict() =
        handleEvent<PostBookmarkRemovedEvent>(postCacheHandlingStream) { event ->
            bookmarkedPostListCache.evictAll(event.memberId)
            postStatsCache.evict(event.postId)
            bookmarkedAllPostIdSetCache.evictAll(event.postId)
        }
}
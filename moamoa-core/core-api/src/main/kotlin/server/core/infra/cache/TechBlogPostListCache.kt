package server.core.infra.cache

import org.springframework.stereotype.Component
import server.core.feature.post.query.PostSummary
import server.cache.CacheMemory
import server.cache.get

@Component
class TechBlogPostListCache(
    private val cacheMemory: server.cache.CacheMemory
) {
    private val prefix = "POST:LIST:TECHBLOG:"
    private val thirtyMinutes: Long = 1_800_000L

    fun key(techBlogId: Long, page: Long) =
        "$prefix$techBlogId:PAGE:$page"

    fun get(techBlogId: Long, page: Long): List<PostSummary>? =
        cacheMemory.get(key(techBlogId, page))

    fun set(techBlogId: Long, page: Long, posts: List<PostSummary>) {
        cacheMemory.set(key(techBlogId, page), posts, thirtyMinutes)
    }
}

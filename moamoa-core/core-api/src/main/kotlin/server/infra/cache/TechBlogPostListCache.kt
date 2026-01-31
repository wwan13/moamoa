package server.infra.cache

import org.springframework.stereotype.Component
import server.cache.CacheMemory
import server.feature.post.query.PostSummary

@Component
class TechBlogPostListCache(
    private val cacheMemory: CacheMemory
) {
    private val prefix = "POST:LIST:TECHBLOG:"
    private val thirtyMinutes: Long = 1_800_000L

    private fun key(techBlogId: Long, page: Long) =
        "$prefix$techBlogId:PAGE:$page"

    suspend fun get(techBlogId: Long, page: Long): List<PostSummary>? =
        cacheMemory.get(key(techBlogId, page))

    suspend fun set(techBlogId: Long, page: Long, posts: List<PostSummary>) {
        cacheMemory.set(key(techBlogId, page), posts, thirtyMinutes)
    }
}
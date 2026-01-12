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

    private fun key(techBlogKey: String, page: Long) =
        "$prefix$techBlogKey:PAGE:$page"

    suspend fun get(techBlogKey: String, page: Long): List<PostSummary>? =
        cacheMemory.get(key(techBlogKey, page))

    suspend fun set(techBlogKey: String, page: Long, posts: List<PostSummary>) {
        cacheMemory.set(key(techBlogKey, page), posts, thirtyMinutes)
    }
}
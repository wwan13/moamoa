package server.infra.cache

import org.springframework.stereotype.Component
import server.cache.CacheMemory
import server.feature.post.query.PostSummary

@Component
class PostListCache(
    private val cacheMemory: CacheMemory
) {

    private val prefix = "POST:LIST:PAGE:"
    private val thirtyMinutes: Long = 1_800_000L

    private fun key(page: Long) = prefix + page

    suspend fun get(page: Long): List<PostSummary>? {
        return cacheMemory.get(key(page))
    }

    suspend fun set(page: Long, posts: List<PostSummary>) {
        cacheMemory.set(key(page), posts, thirtyMinutes)
    }
}
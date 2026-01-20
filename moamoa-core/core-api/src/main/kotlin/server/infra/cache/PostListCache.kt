package server.infra.cache

import org.springframework.stereotype.Component
import server.cache.CacheMemory
import server.feature.post.query.PostSummary

@Component
class PostListCache(
    private val cacheMemory: CacheMemory
) {

    private val prefix = "POST:LIST"
    private val ttlMillis: Long = 1_800_000L // 30분

    private fun key(page: Long, size: Long) =
        "$prefix:PAGE:$page:SIZE:$size"

    suspend fun get(page: Long, size: Long): List<PostSummary>? {
        return cacheMemory.get(key(page, size))
    }

    suspend fun set(
        page: Long,
        size: Long,
        posts: List<PostSummary>
    ) {
        cacheMemory.set(key(page, size), posts, ttlMillis)
    }
}
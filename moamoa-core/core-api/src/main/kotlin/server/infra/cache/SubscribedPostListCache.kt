package server.infra.cache

import org.springframework.stereotype.Component
import server.cache.CacheMemory
import server.feature.post.query.PostSummary

@Component
class SubscribedPostListCache(
    private val cacheMemory: CacheMemory,
) {
    private val prefix = "POST:LIST:SUBSCRIBED:"
    private val ttlMillis: Long = 60_000L

    private fun versionKey(memberId: Long) = "$prefix$memberId:VER"

    private fun key(memberId: Long, version: Long, page: Long) =
        "$prefix$memberId:V:${version}:PAGE:$page:"

    suspend fun get(memberId: Long, page: Long): List<PostSummary>? {
        val ver = cacheMemory.get(versionKey(memberId)) ?: 1L
        return cacheMemory.get(key(memberId, ver, page))
    }

    suspend fun set(memberId: Long, page: Long, posts: List<PostSummary>) {
        val ver = cacheMemory.get(versionKey(memberId)) ?: 1L
        cacheMemory.set(key(memberId,  ver, page), posts, ttlMillis)
    }

    suspend fun evictAll(memberId: Long) {
        cacheMemory.incr(versionKey(memberId))
    }
}
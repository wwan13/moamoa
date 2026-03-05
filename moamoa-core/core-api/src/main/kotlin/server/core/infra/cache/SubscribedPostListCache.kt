package server.core.infra.cache

import org.springframework.stereotype.Component
import server.core.feature.post.query.PostSummary
import server.cache.CacheMemory
import server.cache.get

@Component
class SubscribedPostListCache(
    private val cacheMemory: server.cache.CacheMemory,
) {
    private val prefix = "POST:LIST:SUBSCRIBED:"
    private val ttlMillis: Long = 60_000L

    fun versionKey(memberId: Long) = "$prefix$memberId:VER"

    fun key(memberId: Long, version: Long, page: Long) =
        "$prefix$memberId:V:${version}:PAGE:$page:"

    fun get(memberId: Long, page: Long): List<PostSummary>? {
        val ver = cacheMemory.get(versionKey(memberId)) ?: 1L
        return cacheMemory.get(key(memberId, ver, page))
    }

    fun set(memberId: Long, page: Long, posts: List<PostSummary>) {
        val ver = cacheMemory.get(versionKey(memberId)) ?: 1L
        cacheMemory.set(key(memberId,  ver, page), posts, ttlMillis)
    }

    fun evictAll(memberId: Long) {
        cacheMemory.incr(versionKey(memberId))
    }
}

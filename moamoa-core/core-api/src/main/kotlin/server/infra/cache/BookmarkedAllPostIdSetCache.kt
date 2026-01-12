package server.infra.cache

import org.springframework.stereotype.Component
import server.cache.CacheMemory

@Component
class BookmarkedAllPostIdSetCache(
    private val cacheMemory: CacheMemory,
) {
    private val prefix = "POST:BOOKMARKED:ALL:"
    private val ttlMillis: Long = 60_000L

    private fun versionKey(memberId: Long) = "$prefix$memberId:VER"
    private fun key(memberId: Long, version: Long) = "$prefix$memberId:V:$version"

    suspend fun get(memberId: Long): Set<Long>? {
        val ver = cacheMemory.get<Long>(versionKey(memberId)) ?: 1L
        return cacheMemory.get(key(memberId, ver))
    }

    suspend fun set(memberId: Long, postIds: Set<Long>) {
        val ver = cacheMemory.get<Long>(versionKey(memberId)) ?: 1L
        cacheMemory.set(key(memberId, ver), postIds, ttlMillis)
    }

    suspend fun evictAll(memberId: Long) {
        cacheMemory.incr(versionKey(memberId))
    }
}
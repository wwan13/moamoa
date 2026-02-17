package server.infra.cache

import org.springframework.stereotype.Component
import server.shared.cache.CacheMemory
import server.shared.cache.get
import server.feature.techblog.query.TechBlogSubscriptionInfo

@Component
class TechBlogSubscriptionCache(
    private val cacheMemory: CacheMemory,
) {
    private val prefix = "TECHBLOG:SUBSCRIPTION:ALL:"
    private val ttlMillis: Long = 60_000L

    fun versionKey(memberId: Long) = "$prefix$memberId:VER"
    fun key(memberId: Long, version: Long) = "$prefix$memberId:V:$version"

    suspend fun get(memberId: Long): List<TechBlogSubscriptionInfo>? {
        val ver = cacheMemory.get<Long>(versionKey(memberId)) ?: 1L
        return cacheMemory.get(key(memberId, ver))
    }

    suspend fun set(memberId: Long, value: List<TechBlogSubscriptionInfo>) {
        val ver = cacheMemory.get<Long>(versionKey(memberId)) ?: 1L
        cacheMemory.set(key(memberId, ver), value, ttlMillis)
    }

    suspend fun evictAll(memberId: Long) {
        cacheMemory.incr(versionKey(memberId))
    }
}

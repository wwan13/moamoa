package server.core.infra.cache

import org.springframework.stereotype.Component
import server.core.feature.techblog.query.TechBlogSubscriptionInfo
import server.cache.CacheMemory
import server.cache.get

@Component
class TechBlogSubscriptionCache(
    private val cacheMemory: server.cache.CacheMemory,
) {
    private val prefix = "TECHBLOG:SUBSCRIPTION:ALL:"
    private val ttlMillis: Long = 60_000L

    fun versionKey(memberId: Long) = "$prefix$memberId:VER"
    fun key(memberId: Long, version: Long) = "$prefix$memberId:V:$version"

    fun get(memberId: Long): List<TechBlogSubscriptionInfo>? {
        val ver = cacheMemory.get<Long>(versionKey(memberId)) ?: 1L
        return cacheMemory.get(key(memberId, ver))
    }

    fun set(memberId: Long, value: List<TechBlogSubscriptionInfo>) {
        val ver = cacheMemory.get<Long>(versionKey(memberId)) ?: 1L
        cacheMemory.set(key(memberId, ver), value, ttlMillis)
    }

    fun evictAll(memberId: Long) {
        cacheMemory.incr(versionKey(memberId))
    }
}

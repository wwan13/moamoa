package server.core.infra.cache

import org.springframework.stereotype.Component
import server.core.feature.techblog.query.TechBlogSummary
import server.cache.CacheMemory
import server.cache.get

@Component
class TechBlogListCache(
    private val cacheMemory: server.cache.CacheMemory,
) {
    val key = "TECHBLOG:BASE:LIST"
    private val ttlMillis = 1_800_000L

    fun get(): List<TechBlogSummary>? = cacheMemory.get(key)

    fun set(value: List<TechBlogSummary>) {
        cacheMemory.set(key, value, ttlMillis)
    }

    fun evict() {
        cacheMemory.evict(key)
    }
}

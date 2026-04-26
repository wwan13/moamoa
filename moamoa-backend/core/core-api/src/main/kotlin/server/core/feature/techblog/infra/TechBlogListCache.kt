package server.core.feature.techblog.infra

import org.springframework.stereotype.Component
import server.cache.CacheMemory
import server.cache.get
import server.core.feature.techblog.query.TechBlogSummary

@Component
class TechBlogListCache(
    private val cacheMemory: CacheMemory,
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
package server.infra.cache

import org.springframework.stereotype.Component
import server.shared.cache.CacheMemory
import server.shared.cache.get
import server.feature.techblog.query.TechBlogSummary

@Component
class TechBlogListCache(
    private val cacheMemory: CacheMemory,
) {
    val key = "TECHBLOG:BASE:LIST"
    private val ttlMillis = 1_800_000L

    suspend fun get(): List<TechBlogSummary>? = cacheMemory.get(key)

    suspend fun set(value: List<TechBlogSummary>) {
        cacheMemory.set(key, value, ttlMillis)
    }

    suspend fun evict() {
        cacheMemory.evict(key)
    }
}

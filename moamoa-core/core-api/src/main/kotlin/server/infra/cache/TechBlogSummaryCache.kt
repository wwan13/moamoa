package server.infra.cache

import org.springframework.stereotype.Component
import server.cache.CacheMemory
import server.feature.techblog.query.TechBlogSummary

@Component
class TechBlogSummaryCache(
    private val cacheMemory: CacheMemory,
) {
    private val prefix = "TECHBLOG:SUMMARY:"
    private val ttlMillis: Long = 60_000L

    private fun key(id: Long) = "$prefix$id"

    suspend fun get(id: Long): TechBlogSummary? =
        cacheMemory.get(key(id))

    suspend fun mGet(ids: Collection<Long>): Map<Long, TechBlogSummary?> {
        if (ids.isEmpty()) return emptyMap()

        val idByKey = ids.distinct().associateBy { key(it) }
        val raw = cacheMemory.mgetAs<TechBlogSummary>(idByKey.keys)

        return idByKey.entries.associate { (k, id) ->
            id to raw[k]
        }
    }

    suspend fun set(summary: TechBlogSummary) {
        cacheMemory.set(key(summary.id), normalize(summary), ttlMillis)
    }

    suspend fun mSet(summaries: Map<Long, TechBlogSummary>) {
        if (summaries.isEmpty()) return

        val payload = summaries.entries.associate { (id, s) ->
            key(id) to normalize(s)
        }
        cacheMemory.mset(payload, ttlMillis)
    }

    private fun normalize(s: TechBlogSummary) = s.copy(
        subscribed = false,
        notificationEnabled = false,
    )

    suspend fun evict(id: Long) {
        cacheMemory.evict(key(id))
    }
}
package server.core.infra.cache

import org.springframework.stereotype.Component
import server.core.feature.techblog.query.TechBlogSummary
import server.cache.CacheMemory
import server.cache.get
import server.cache.mgetAs

@Component
class TechBlogSummaryCache(
    private val cacheMemory: server.cache.CacheMemory,
) {
    private val prefix = "TECHBLOG:SUMMARY:"
    private val ttlMillis: Long = 60_000L

    fun key(id: Long) = "$prefix$id"

    fun get(id: Long): TechBlogSummary? =
        cacheMemory.get(key(id))

    fun mGet(ids: Collection<Long>): Map<Long, TechBlogSummary?> {
        if (ids.isEmpty()) return emptyMap()

        val idByKey = ids.distinct().associateBy { key(it) }
        val raw = cacheMemory.mgetAs<TechBlogSummary>(idByKey.keys)

        return idByKey.entries.associate { (k, id) ->
            id to raw[k]
        }
    }

    fun set(summary: TechBlogSummary) {
        cacheMemory.set(key(summary.id), normalize(summary), ttlMillis)
    }

    fun mSet(summaries: Map<Long, TechBlogSummary>) {
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

    fun evict(id: Long) {
        cacheMemory.evict(key(id))
    }
}

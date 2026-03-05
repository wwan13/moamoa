package server.core.infra.cache

import org.springframework.stereotype.Component
import server.core.feature.post.query.PostStats
import server.cache.CacheMemory
import server.cache.get
import server.cache.mgetAs

@Component
class PostStatsCache(
    private val cacheMemory: server.cache.CacheMemory,
) {
    private val prefix = "POST:STATS:"
    private val ttlMillis: Long = 60_000L

    fun key(postId: Long) = "$prefix$postId"

    fun get(postId: Long): PostStats? {
        return cacheMemory.get(key(postId))
    }

    fun mGet(postIds: Collection<Long>): Map<Long, PostStats?> {
        if (postIds.isEmpty()) return emptyMap()

        val idByKey = postIds.distinct().associateBy { key(it) }
        val raw = cacheMemory.mgetAs<PostStats>(idByKey.keys)

        return idByKey.entries.associate { (k, id) ->
            id to raw[k]
        }
    }

    fun set(postId: Long, postStats: PostStats) {
        cacheMemory.set(key(postId), postStats, ttlMillis)
    }

    fun mSet(statsByPostId: Map<Long, PostStats>) {
        if (statsByPostId.isEmpty()) return

        val payload = statsByPostId.entries.associate { (postId, stats) ->
            key(postId) to stats
        }

        cacheMemory.mset(payload, ttlMillis)
    }

    fun evict(postId: Long) {
        cacheMemory.evict(key(postId))
    }
}

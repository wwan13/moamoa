package server.core.feature.post.query

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import server.core.feature.post.infra.PostStatsCache
import server.core.infra.cache.WarmupCoordinator

@Component
class PostStatsReader(
    private val jdbc: NamedParameterJdbcTemplate,
    private val postStatsCache: PostStatsCache,
    private val warmupCoordinator: WarmupCoordinator,
) {

    fun findPostStatsMap(postIds: List<Long>): Map<Long, PostStats> {
        if (postIds.isEmpty()) return emptyMap()

        val cachedMap = postStatsCache.mGet(postIds)
        val missedIds = postIds.filter { cachedMap[it] == null }

        val dbMap = if (missedIds.isNotEmpty()) {
            fetchPostStatsMap(missedIds)
        } else {
            emptyMap()
        }

        if (dbMap.isNotEmpty()) {
            val cacheKeys = dbMap.keys.map(postStatsCache::key)
            val warmupKey = WarmupCoordinator.Companion.msetKey("PostStatsCache", cacheKeys)
            warmupCoordinator.launchIfAbsent(warmupKey) {
                postStatsCache.mSet(dbMap)
            }
        }

        val result = LinkedHashMap<Long, PostStats>(postIds.size)
        postIds.forEach { id ->
            val v = cachedMap[id] ?: dbMap[id]
            if (v != null) result[id] = v
        }
        return result
    }

    private fun fetchPostStatsMap(postIds: List<Long>): Map<Long, PostStats> {
        if (postIds.isEmpty()) return emptyMap()

        val placeholders = postIds.indices.joinToString(",") { ":id$it" }

        val sql = """
            SELECT 
                p.id AS post_id,
                p.bookmark_count AS bookmark_count,
                p.view_count AS view_count
            FROM post p
            WHERE p.id IN ($placeholders)
        """.trimIndent()

        val params = MapSqlParameterSource()
        postIds.forEachIndexed { idx, id -> params.addValue("id$idx", id) }

        val rows: List<PostStats> = jdbc.query(sql, params) { row, _ ->
                PostStats(
                    postId = row.getLong("post_id"),
                    bookmarkCount = row.getLong("bookmark_count"),
                    viewCount = row.getLong("view_count"),
                )
            }

        return rows.associateBy { it.postId }
    }
}

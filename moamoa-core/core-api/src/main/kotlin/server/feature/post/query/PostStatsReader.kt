package server.feature.post.query

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Component
import server.infra.cache.PostStatsCache

@Component
class PostStatsReader(
    private val databaseClient: DatabaseClient,
    private val postStatsCache: PostStatsCache,
    private val cacheWarmupScope: CoroutineScope,
) {

    suspend fun findPostStatsMap(postIds: List<Long>): Map<Long, PostStats> {
        if (postIds.isEmpty()) return emptyMap()

        val cachedMap = postStatsCache.mGet(postIds)
        val missedIds = postIds.filter { cachedMap[it] == null }

        val dbMap = if (missedIds.isNotEmpty()) {
            fetchPostStatsMap(missedIds)
        } else {
            emptyMap()
        }

        if (dbMap.isNotEmpty()) {
            cacheWarmupScope.launch {
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

    private suspend fun fetchPostStatsMap(postIds: List<Long>): Map<Long, PostStats> {
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

        var spec = databaseClient.sql(sql)
        postIds.forEachIndexed { idx, id -> spec = spec.bind("id$idx", id) }

        return spec
            .map { row, _ ->
                PostStats(
                    postId = row.get("post_id", Long::class.java) ?: 0L,
                    bookmarkCount = row.get("bookmark_count", Long::class.java) ?: 0L,
                    viewCount = row.get("view_count", Long::class.java) ?: 0L,
                )
            }
            .all()
            .asFlow()
            .toList()
            .associateBy { it.postId }
    }
}
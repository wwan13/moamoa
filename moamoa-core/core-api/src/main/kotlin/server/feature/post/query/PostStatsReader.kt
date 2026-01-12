package server.feature.post.query

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Component

@Component
class PostStatsReader(
    private val databaseClient: DatabaseClient,
) {

    suspend fun findBookmarkCountMap(postIds: List<Long>): Map<Long, PostStats> {
        if (postIds.isEmpty()) return emptyMap()

        val placeholders = postIds.indices.joinToString(",") { ":id$it" }

        val sql = """
            SELECT 
                p.id AS post_id,
                p.bookmark_count AS bookmark_count
                p.view_count AS view_count
            FROM post p
            WHERE p.id IN ($placeholders)
        """.trimIndent()

        var spec = databaseClient.sql(sql)

        postIds.forEachIndexed { idx, id ->
            spec = spec.bind("id$idx", id)
        }

        return spec
            .map { row, _ ->
                val postId = row.get("post_id", Long::class.java) ?: 0L
                val bookmarkCount = row.get("bookmark_count", Long::class.java) ?: 0L
                val viewCount = row.get("view_count", Long::class.java) ?: 0L
                PostStats(
                    postId = postId,
                    bookmarkCount = bookmarkCount,
                    viewCount = viewCount,
                )
            }
            .all()
            .asFlow()
            .toList()
            .associateBy { it.postId }
    }
}
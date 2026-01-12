package server.feature.post.query

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Component

@Component
class PostBookmarkCountReader(
    private val databaseClient: DatabaseClient,
) {

    suspend fun findBookmarkCountMap(postIds: List<Long>): Map<Long, Long> {
        if (postIds.isEmpty()) return emptyMap()

        val placeholders = postIds.indices.joinToString(",") { ":id$it" }

        val sql = """
            SELECT 
                p.id AS post_id,
                p.bookmark_count AS bookmark_count
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
                val count = row.get("bookmark_count", Long::class.java) ?: 0L
                postId to count
            }
            .all()
            .asFlow()
            .toList()
            .toMap()
    }
}
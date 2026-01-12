package server.feature.post.query

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.reactive.asFlow
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Component

@Component
class BookmarkedPostReader(
    private val databaseClient: DatabaseClient,
) {

    suspend fun findBookmarkedPostIdSet(
        memberId: Long,
        postIds: List<Long>,
    ): Set<Long> {
        if (postIds.isEmpty()) return emptySet()

        val placeholders = postIds.indices.joinToString(",") { ":id$it" }

        val sql = """
            SELECT pb.post_id AS post_id
            FROM post_bookmark pb
            WHERE pb.member_id = :memberId
              AND pb.post_id IN ($placeholders)
        """.trimIndent()

        var spec = databaseClient.sql(sql)
            .bind("memberId", memberId)

        postIds.forEachIndexed { idx, id ->
            spec = spec.bind("id$idx", id)
        }

        return spec
            .map { row, _ -> row.get("post_id", Long::class.java) ?: 0L }
            .all()
            .asFlow()
            .toSet()
    }
}
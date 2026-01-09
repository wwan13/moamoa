package server.feature.post.query

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Service
import server.security.Passport
import support.paging.Paging
import support.paging.calculateTotalPage

@Service
class PostQueryService(
    private val databaseClient: DatabaseClient,
) {

    suspend fun findByConditions(
        conditions: PostQueryConditions,
        passport: Passport?
    ): PostList {
        val paging = Paging(
            size = conditions.size ?: 20,
            page = conditions.page ?: 1
        )

        val totalCount = countAll()

        val meta = PostListMeta(
            page = paging.page,
            size = paging.size,
            totalCount = totalCount,
            totalPages = calculateTotalPage(totalCount, paging.size)
        )

        val posts = findAll(
            paging = paging,
            memberId = passport?.memberId
        ).toList()

        return PostList(meta, posts)
    }

    private suspend fun findAll(
        paging: Paging,
        memberId: Long?
    ): Flow<PostSummary> {
        val offset = (paging.page - 1L) * paging.size

        val sql = if (memberId != null) {
            """
                $POST_QUERY_BASE_SELECT,
                    (pb.post_id IS NOT NULL) AS is_bookmarked
                FROM post p
                INNER JOIN tech_blog t ON t.id = p.tech_blog_id
                LEFT JOIN post_bookmark pb
                  ON pb.post_id = p.id
                 AND pb.member_id = :memberId
                ORDER BY p.published_at DESC
                LIMIT :limit OFFSET :offset
            """.trimIndent()
        } else {
            """
                $POST_QUERY_BASE_SELECT,
                    FALSE AS is_bookmarked
                FROM post p
                INNER JOIN tech_blog t ON t.id = p.tech_blog_id
                ORDER BY p.published_at DESC
                LIMIT :limit OFFSET :offset
            """.trimIndent()
        }

        var spec = databaseClient.sql(sql)
            .bind("limit", paging.size)
            .bind("offset", offset)

        if (memberId != null) spec = spec.bind("memberId", memberId)

        return spec
            .map { row, _ -> mapToPostSummary(row) }
            .all()
            .asFlow()
    }

    private suspend fun countAll(): Long {
        val sql = """
            SELECT COUNT(*) AS cnt
            FROM post
        """.trimIndent()

        return databaseClient.sql(sql)
            .map { row, _ -> row.get("cnt", Long::class.java) ?: 0L }
            .one()
            .awaitSingle()
    }
}
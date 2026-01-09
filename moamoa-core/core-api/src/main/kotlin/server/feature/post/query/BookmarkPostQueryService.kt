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
class BookmarkPostQueryService(
    private val databaseClient: DatabaseClient,
) {

    suspend fun findAllByConditions(
        conditions: PostQueryConditions,
        passport: Passport,
    ): PostList {
        val paging = Paging(
            size = conditions.size ?: 20,
            page = conditions.page ?: 1
        )

        val totalCount = countBookmarkedPosts(memberId = passport.memberId)

        val meta = PostListMeta(
            page = paging.page,
            size = paging.size,
            totalCount = totalCount,
            totalPages = calculateTotalPage(totalCount, paging.size)
        )

        val posts = findBookmarkedPosts(
            paging = paging,
            memberId = passport.memberId
        ).toList()

        return PostList(meta, posts)
    }

    private suspend fun findBookmarkedPosts(
        paging: Paging,
        memberId: Long,
    ): Flow<PostSummary> {
        val offset = (paging.page - 1L) * paging.size

        val sql = """
            $POST_QUERY_BASE_SELECT,
                TRUE AS is_bookmarked
            FROM post_bookmark pb
            INNER JOIN post p ON p.id = pb.post_id
            INNER JOIN tech_blog t ON t.id = p.tech_blog_id
            WHERE pb.member_id = :memberId
            ORDER BY p.published_at DESC
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return databaseClient.sql(sql)
            .bind("memberId", memberId)
            .bind("limit", paging.size)
            .bind("offset", offset)
            .map { row, _ -> mapToPostSummary(row) }
            .all()
            .asFlow()
    }

    private suspend fun countBookmarkedPosts(memberId: Long): Long {
        val sql = """
            SELECT COUNT(*) AS cnt
            FROM post_bookmark pb
            WHERE pb.member_id = :memberId
        """.trimIndent()

        return databaseClient.sql(sql)
            .bind("memberId", memberId)
            .map { row, _ -> row.get("cnt", Long::class.java) ?: 0L }
            .one()
            .awaitSingle()
    }
}
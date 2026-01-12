package server.feature.post.query

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Service
import server.infra.cache.SubscribedPostListCache
import server.security.Passport
import support.paging.Paging
import support.paging.calculateTotalPage

@Service
class SubscribedPostQueryService(
    private val databaseClient: DatabaseClient,
    private val subscribedPostListCache: SubscribedPostListCache
) {

    suspend fun findAllByConditions(
        conditions: PostQueryConditions,
        passport: Passport,
    ): PostList {
        val paging = Paging(
            size = conditions.size ?: 20,
            page = conditions.page ?: 1
        )

        val totalCount = countSubscribingPosts(memberId = passport.memberId)

        val meta = PostListMeta(
            page = paging.page,
            size = paging.size,
            totalCount = totalCount,
            totalPages = calculateTotalPage(totalCount, paging.size)
        )

        val posts = loadPosts(
            paging = paging,
            memberId = passport.memberId
        )

        return PostList(meta, posts)
    }

    private suspend fun loadPosts(memberId: Long, paging: Paging): List<PostSummary> {
        if (paging.page > 5) {
            return findSubscribingPosts(paging, memberId).toList()
        }

        val fromCache = subscribedPostListCache.get(memberId, paging.page)
        if (fromCache != null) {
            return fromCache
        }

        return findSubscribingPosts(paging, memberId).toList().also {
            subscribedPostListCache.set(memberId, paging.page, it)
        }
    }

    private suspend fun findSubscribingPosts(
        paging: Paging,
        memberId: Long,
    ): Flow<PostSummary> {
        val offset = (paging.page - 1L) * paging.size

        val sql = """
            $POST_QUERY_BASE_SELECT,
                (pb.post_id IS NOT NULL) AS is_bookmarked
            FROM tech_blog_subscription s
            INNER JOIN tech_blog t ON t.id = s.tech_blog_id
            INNER JOIN post p ON p.tech_blog_id = t.id
            LEFT JOIN post_bookmark pb
              ON pb.post_id = p.id
             AND pb.member_id = :memberId
            WHERE s.member_id = :memberId
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

    private suspend fun countSubscribingPosts(memberId: Long): Long {
        val sql = """
            SELECT COUNT(*) AS cnt
            FROM tech_blog_subscription s
            INNER JOIN post p ON p.tech_blog_id = s.tech_blog_id
            WHERE s.member_id = :memberId
        """.trimIndent()

        return databaseClient.sql(sql)
            .bind("memberId", memberId)
            .map { row, _ -> row.get("cnt", Long::class.java) ?: 0L }
            .one()
            .awaitSingle()
    }
}
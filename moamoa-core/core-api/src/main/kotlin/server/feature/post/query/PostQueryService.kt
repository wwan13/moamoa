package server.feature.post.query

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Service
import server.feature.techblog.application.TechBlogData
import server.security.Passport
import support.paging.Paging
import support.paging.calculateTotalPage
import java.time.LocalDateTime

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

        val totalCount = countByConditions(
            techBlogId = conditions.techBlogId
        )

        val meta = PostListMeta(
            page = paging.page,
            size = paging.size,
            totalCount = totalCount,
            totalPages = calculateTotalPage(totalCount, paging.size)
        )

        val posts = findAllByConditions(
            paging = paging,
            techBlogId = conditions.techBlogId,
            memberId = passport?.memberId
        ).toList()

        return PostList(meta, posts)
    }

    private suspend fun findAllByConditions(
        paging: Paging,
        techBlogId: Long?,
        memberId: Long?
    ): Flow<PostSummary> {
        val offset = (paging.page - 1L) * paging.size

        val whereClause = buildString {
            append("WHERE 1=1 ")
            if (techBlogId != null) append("AND p.tech_blog_id = :techBlogId ")
        }

        val baseSelect = """
            SELECT
                p.id               AS post_id,
                p.post_key         AS post_key,
                p.title            AS post_title,
                p.description      AS post_description,
                p.thumbnail        AS post_thumbnail,
                p.url              AS post_url,
                p.published_at     AS published_at,
                p.view_count       AS post_view_count,
                p.bookmark_count   AS post_bookmark_count,

                t.id               AS tech_blog_id,
                t.title            AS tech_blog_title,
                t.tech_blog_key    AS tech_blog_key,
                t.blog_url         AS tech_blog_url,
                t.icon             AS tech_blog_icon,
                t.subscription_count AS tech_blog_subscription_count
        """.trimIndent()

        val sql = if (memberId != null) {
            """
                $baseSelect,
                    (pb.post_id IS NOT NULL) AS is_bookmarked
                FROM post p
                INNER JOIN tech_blog t ON t.id = p.tech_blog_id
                LEFT JOIN post_bookmark pb
                  ON pb.post_id = p.id
                 AND pb.member_id = :memberId
                $whereClause
                ORDER BY p.published_at DESC
                LIMIT :limit OFFSET :offset
            """.trimIndent()
        } else {
            """
                $baseSelect,
                    FALSE AS is_bookmarked
                FROM post p
                INNER JOIN tech_blog t ON t.id = p.tech_blog_id
                $whereClause
                ORDER BY p.published_at DESC
                LIMIT :limit OFFSET :offset
            """.trimIndent()
        }

        var spec = databaseClient.sql(sql)
            .bind("limit", paging.size)
            .bind("offset", offset)

        if (memberId != null) spec = spec.bind("memberId", memberId)
        if (techBlogId != null) spec = spec.bind("techBlogId", techBlogId)

        return spec
            .map { row, _ ->
                PostSummary(
                    id = row.get("post_id", Long::class.java) ?: 0L,
                    key = row.get("post_key", String::class.java).orEmpty(),
                    title = row.get("post_title", String::class.java).orEmpty(),
                    description = row.get("post_description", String::class.java).orEmpty(),
                    thumbnail = row.get("post_thumbnail", String::class.java).orEmpty(),
                    url = row.get("post_url", String::class.java).orEmpty(),
                    publishedAt = row.get("published_at", LocalDateTime::class.java) ?: LocalDateTime.MIN,
                    viewCount = row.get("post_view_count", Long::class.java) ?: 0L,
                    bookmarkCount = row.get("post_bookmark_count", Long::class.java) ?: 0L,
                    isBookmarked = (row.get("is_bookmarked", Int::class.java) == 1),
                    techBlog = TechBlogData(
                        id = row.get("tech_blog_id", Long::class.java) ?: 0L,
                        title = row.get("tech_blog_title", String::class.java).orEmpty(),
                        key = row.get("tech_blog_key", String::class.java).orEmpty(),
                        blogUrl = row.get("tech_blog_url", String::class.java).orEmpty(),
                        icon = row.get("tech_blog_icon", String::class.java).orEmpty(),
                        subscriptionCount = row.get("tech_blog_subscription_count", Long::class.java) ?: 0L
                    )
                )
            }
            .all()
            .asFlow()
    }

    private suspend fun countByConditions(
        techBlogId: Long?
    ): Long {
        val whereClause = buildString {
            append("WHERE 1=1 ")
            if (techBlogId != null) append("AND tech_blog_id = :techBlogId ")
        }

        val sql = """
            SELECT COUNT(*) AS cnt
            FROM post
            $whereClause
        """.trimIndent()

        var spec = databaseClient.sql(sql)
        if (techBlogId != null) spec = spec.bind("techBlogId", techBlogId)

        return spec
            .map { row, _ -> row.get("cnt", Long::class.java) ?: 0L }
            .one()
            .awaitSingle()
    }
}
package server.application.query

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import server.application.PostSummary
import server.application.TechBlogData
import support.paging.Paging
import java.time.LocalDateTime

@Repository
class PostQueryRepository(
    private val databaseClient: DatabaseClient
) {

    suspend fun findAllByConditions(paging: Paging): Flow<PostSummary> {
        val offset = (paging.page - 1L) * paging.size

        val sql = """
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
            FROM post p
            INNER JOIN tech_blog t ON t.id = p.tech_blog_id
            ORDER BY p.published_at DESC
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return databaseClient.sql(sql)
            .bind("limit", paging.size)
            .bind("offset", offset)
            .map { row, _ ->
                PostSummary(
                    id = (row.get("post_id", Long::class.java) ?: 0L).toLong(),
                    key = row.get("post_key", String::class.java).orEmpty(),
                    title = row.get("post_title", String::class.java).orEmpty(),
                    description = row.get("post_description", String::class.java).orEmpty(),
                    thumbnail = row.get("post_thumbnail", String::class.java).orEmpty(),
                    url = row.get("post_url", String::class.java).orEmpty(),
                    publishedAt = row.get("published_at", LocalDateTime::class.java) ?: LocalDateTime.MIN,
                    viewCount = (row.get("post_view_count", java.lang.Long::class.java) ?: 0L).toLong(),
                    bookmarkCount = (row.get("post_bookmark_count", java.lang.Long::class.java) ?: 0L).toLong(),
                    techBlog = TechBlogData(
                        id = (row.get("tech_blog_id", Long::class.java) ?: 0L).toLong(),
                        title = row.get("tech_blog_title", String::class.java).orEmpty(),
                        key = row.get("tech_blog_key", String::class.java).orEmpty(),
                        blogUrl = row.get("tech_blog_url", String::class.java).orEmpty(),
                        icon = row.get("tech_blog_icon", String::class.java).orEmpty(),
                        subscriptionCount = (row.get("tech_blog_subscription_count", Long::class.java) ?: 0L)
                    )
                )
            }
            .all()
            .asFlow()
    }

    suspend fun countByConditions(): Long {
        val sql = "SELECT COUNT(*) AS cnt FROM post"

        return databaseClient.sql(sql)
            .map { row, _ -> (row.get("cnt", java.lang.Long::class.java) ?: 0L).toLong() }
            .one()
            .awaitSingle()
    }
}
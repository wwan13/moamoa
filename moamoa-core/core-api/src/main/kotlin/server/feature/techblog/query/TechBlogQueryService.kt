package server.feature.techblog.query

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Service
import server.security.Passport

@Service
class TechBlogQueryService(
    private val databaseClient: DatabaseClient
) {

    suspend fun findAll(passport: Passport?): TechBlogList {
        val memberId = passport?.memberId

        val techBlogs = findList(memberId).toList()
        val meta = TechBlogListMeta(
            totalCount = techBlogs.size.toLong(),
        )

        return TechBlogList(meta, techBlogs)
    }

    private suspend fun findList(memberId: Long?): Flow<TechBlogSummary> {
        val sql = if (memberId != null) {
            """
                $TECH_BLOG_QUERY_BASE_SELECT,
                    (s.tech_blog_id IS NOT NULL) AS is_subscribed,
                    COALESCE(s.notification_enabled, 0) AS notification_enabled
                FROM tech_blog t
                LEFT JOIN (
                    SELECT tech_blog_id, COUNT(*) AS post_count
                    FROM post
                    GROUP BY tech_blog_id
                ) pc ON pc.tech_blog_id = t.id
                LEFT JOIN tech_blog_subscription s
                  ON s.tech_blog_id = t.id
                 AND s.member_id = :memberId
                ORDER BY t.title ASC
            """.trimIndent()
        } else {
            """
                $TECH_BLOG_QUERY_BASE_SELECT,
                    FALSE AS is_subscribed,
                    0 AS notification_enabled
                FROM tech_blog t
                LEFT JOIN (
                    SELECT tech_blog_id, COUNT(*) AS post_count
                    FROM post
                    GROUP BY tech_blog_id
                ) pc ON pc.tech_blog_id = t.id
                ORDER BY t.title ASC
            """.trimIndent()
        }

        var spec = databaseClient.sql(sql)

        if (memberId != null) {
            spec = spec.bind("memberId", memberId)
        }

        return spec
            .map { row, _ -> mapToTechBlogSummary(row) }
            .all()
            .asFlow()
    }
}
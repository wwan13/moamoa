package server.feature.techblog.query

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Service
import server.security.Passport

@Service
class SubscribingTechBlogQueryService(
    private val databaseClient: DatabaseClient
) {

    suspend fun findSubscribingTechBlogs(passport: Passport): TechBlogList {
        val techBlogs = findList(passport.memberId).toList()
        val meta = TechBlogListMeta(
            totalCount = techBlogs.size.toLong(),
        )
        return TechBlogList(meta, techBlogs)
    }

    private fun findList(memberId: Long): Flow<TechBlogSummary> {
        val sql = """
            $TECH_BLOG_QUERY_BASE_SELECT,
                1 AS is_subscribed,
                COALESCE(s.notification_enabled, 0) AS notification_enabled
            FROM tech_blog t
            INNER JOIN tech_blog_subscription s
              ON s.tech_blog_id = t.id
             AND s.member_id = :memberId
            LEFT JOIN (
                SELECT tech_blog_id, COUNT(*) AS post_count
                FROM post
                GROUP BY tech_blog_id
            ) pc ON pc.tech_blog_id = t.id
            ORDER BY t.title ASC
        """.trimIndent()

        return databaseClient.sql(sql)
            .bind("memberId", memberId)
            .map { row, _ -> mapToTechBlogSummary(row) }
            .all()
            .asFlow()
    }
}
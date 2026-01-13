package server.feature.techblog.query

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Component
import server.infra.cache.TechBlogSummaryCache

@Component
class TechBlogStatsReader(
    private val databaseClient: DatabaseClient,
    private val techBlogSummaryCache: TechBlogSummaryCache,
) {

    suspend fun findTechBlogStatsMap(techBlogIds: List<Long>): Map<Long, TechBlogStats> {
        if (techBlogIds.isEmpty()) return emptyMap()

        val cached = techBlogSummaryCache.mGet(techBlogIds)
        val missedIds = techBlogIds.filter { cached[it] == null }

        val dbMap = if (missedIds.isNotEmpty()) fetchSummaryMap(missedIds) else emptyMap()
        if (dbMap.isNotEmpty()) techBlogSummaryCache.mSet(dbMap)

        val result = LinkedHashMap<Long, TechBlogStats>(techBlogIds.size)
        techBlogIds.forEach { id ->
            val s = cached[id] ?: dbMap[id]
            if (s != null) {
                result[id] = TechBlogStats(
                    techBlogId = s.id,
                    subscriptionCount = s.subscriptionCount,
                    postCount = s.postCount,
                )
            }
        }
        return result
    }

    private suspend fun fetchSummaryMap(ids: List<Long>): Map<Long, TechBlogSummary> {
        if (ids.isEmpty()) return emptyMap()

        val placeholders = ids.indices.joinToString(",") { ":id$it" }

        val sql = """
            SELECT
                t.id AS tech_blog_id,
                t.title AS tech_blog_title,
                t.icon AS tech_blog_icon,
                t.blog_url AS tech_blog_url,
                t.tech_blog_key AS tech_blog_key,
                t.subscription_count AS subscription_count,
                COALESCE(pc.post_count, 0) AS post_count
            FROM tech_blog t
            LEFT JOIN (
                SELECT tech_blog_id, COUNT(*) AS post_count
                FROM post
                GROUP BY tech_blog_id
            ) pc ON pc.tech_blog_id = t.id
            WHERE t.id IN ($placeholders)
        """.trimIndent()

        var spec = databaseClient.sql(sql)
        ids.forEachIndexed { i, id -> spec = spec.bind("id$i", id) }

        return spec
            .map { row, _ ->
                TechBlogSummary(
                    id = row.get("tech_blog_id", Long::class.java) ?: 0L,
                    title = row.get("tech_blog_title", String::class.java).orEmpty(),
                    icon = row.get("tech_blog_icon", String::class.java).orEmpty(),
                    blogUrl = row.get("tech_blog_url", String::class.java).orEmpty(),
                    key = row.get("tech_blog_key", String::class.java).orEmpty(),
                    subscriptionCount = row.get("subscription_count", Long::class.java) ?: 0L,
                    postCount = row.get("post_count", Long::class.java) ?: 0L,
                    subscribed = false,
                    notificationEnabled = false,
                )
            }
            .all()
            .asFlow()
            .toList()
            .associateBy { it.id }
    }
}
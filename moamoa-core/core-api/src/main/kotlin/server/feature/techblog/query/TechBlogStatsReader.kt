package server.feature.techblog.query

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Component
import server.infra.cache.TechBlogSummaryCache
import server.infra.cache.WarmupCoordinator

@Component
class TechBlogStatsReader(
    private val databaseClient: DatabaseClient,
    private val techBlogSummaryCache: TechBlogSummaryCache,
    private val warmupCoordinator: WarmupCoordinator,
) {

    suspend fun findTechBlogStatsMap(techBlogIds: List<Long>): Map<Long, TechBlogStats> {
        if (techBlogIds.isEmpty()) return emptyMap()

        val cached = techBlogSummaryCache.mGet(techBlogIds)
        val missedIds = techBlogIds.filter { cached[it] == null }

        val dbMap = if (missedIds.isNotEmpty()) fetchTechBlogSummaryMap(missedIds) else emptyMap()
        if (dbMap.isNotEmpty()) {
            val cacheKeys = dbMap.keys.map(techBlogSummaryCache::key)
            val warmupKey = WarmupCoordinator.msetKey("TechBlogSummaryCache", cacheKeys)
            warmupCoordinator.launchIfAbsent(warmupKey) {
                techBlogSummaryCache.mSet(dbMap)
            }
        }

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

    private suspend fun fetchTechBlogSummaryMap(ids: List<Long>): Map<Long, TechBlogSummary> {
        if (ids.isEmpty()) return emptyMap()

        val placeholders = ids.indices.joinToString(",") { ":id$it" }

        val sql = """
                $TECH_BLOG_QUERY_BASE_SELECT
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
            .map { row, _ -> mapToTechBlogSummary(row) }
            .all()
            .asFlow()
            .toList()
            .associateBy { it.id }
    }

    suspend fun findById(techBlogId: Long): TechBlogStats? {
        val cached = techBlogSummaryCache.get(techBlogId)
        if (cached != null) {
            return TechBlogStats(
                techBlogId = cached.id,
                subscriptionCount = cached.subscriptionCount,
                postCount = cached.postCount,
            )
        }

        val summary = fetchTechBlogSummaryById(techBlogId) ?: return null

        val warmupKey = techBlogSummaryCache.key(summary.id)
        warmupCoordinator.launchIfAbsent(warmupKey) {
            techBlogSummaryCache.set(summary)
        }

        return TechBlogStats(
            techBlogId = summary.id,
            subscriptionCount = summary.subscriptionCount,
            postCount = summary.postCount,
        )
    }

    private suspend fun fetchTechBlogSummaryById(id: Long): TechBlogSummary? {
        val sql = """
            $TECH_BLOG_QUERY_BASE_SELECT
            FROM tech_blog t
            LEFT JOIN (
                SELECT tech_blog_id, COUNT(*) AS post_count
                FROM post
                GROUP BY tech_blog_id
            ) pc ON pc.tech_blog_id = t.id
            WHERE t.id = :id
            LIMIT 1
        """.trimIndent()

        return databaseClient.sql(sql)
            .bind("id", id)
            .map { row, _ -> mapToTechBlogSummary(row) }
            .one()
            .asFlow()
            .toList()
            .firstOrNull()
    }
}

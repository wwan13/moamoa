package server.feature.techblog.query

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Service
import server.infra.cache.TechBlogSummaryCache
import server.security.Passport
import kotlin.collections.emptyMap

@Service
class SubscribedTechBlogQueryService(
    private val databaseClient: DatabaseClient,
    private val subscribedTechBlogReader: SubscribedTechBlogReader,
    private val techBlogSummaryCache: TechBlogSummaryCache,
) {

    suspend fun findSubscribingTechBlogs(passport: Passport): TechBlogList {
        val techBlogs = loadAll(passport.memberId)
        val meta = TechBlogListMeta(totalCount = techBlogs.size.toLong())
        return TechBlogList(meta, techBlogs)
    }

    private suspend fun loadAll(memberId: Long): List<TechBlogSummary> {
        val subscriptions: List<TechBlogSubscriptionInfo> =
            subscribedTechBlogReader.findAllSubscribedList(memberId)

        if (subscriptions.isEmpty()) return emptyList()

        val techBlogIds: List<Long> = subscriptions.map { it.techBlogId }
        val subscriptionMap: Map<Long, TechBlogSubscriptionInfo> =
            subscriptions.associateBy { it.techBlogId }

        val cached = techBlogSummaryCache.mGet(techBlogIds)

        val missedIds = techBlogIds.filter { cached[it] == null }.distinct()
        val fetchedMap = if (missedIds.isEmpty()) {
            emptyMap()
        } else {
            val fetched = fetchTechBlogSummaries(missedIds)
            val map = fetched.associateBy { it.id }
            techBlogSummaryCache.mSet(map)
            map
        }

        val baseMap = buildMap {
            cached.forEach { (id, v) -> if (v != null) put(id, v) }
            putAll(fetchedMap)
        }

        return techBlogIds
            .map { id ->
                val base = baseMap[id] ?: return@map null
                val subscriptionInfo = subscriptionMap.getValue(id)

                base.copy(
                    subscribed = true,
                    notificationEnabled = subscriptionInfo.notificationEnabled,
                )
            }
            .filterNotNull()
            .toList()
            .sortedBy { it.title }
    }

    private suspend fun fetchTechBlogSummaries(techBlogIds: List<Long>): List<TechBlogSummary> {
        if (techBlogIds.isEmpty()) return emptyList()

        val placeholders = techBlogIds.indices.joinToString(",") { ":id$it" }

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
        techBlogIds.forEachIndexed { i, id -> spec = spec.bind("id$i", id) }

        return spec
            .map { row, _ -> mapToTechBlogSummary(row) }
            .all()
            .asFlow()
            .toList()
    }
}
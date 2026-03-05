package server.core.feature.techblog.query

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import server.core.infra.cache.TechBlogSummaryCache
import server.core.infra.cache.WarmupCoordinator
import server.core.global.security.Passport
import kotlin.collections.emptyMap
import kotlin.collections.forEach

@Service
class SubscribedTechBlogQueryService(
    private val jdbc: NamedParameterJdbcTemplate,
    private val subscribedTechBlogReader: SubscribedTechBlogReader,
    private val techBlogSummaryCache: TechBlogSummaryCache,
    private val warmupCoordinator: WarmupCoordinator,
) {

    fun findSubscribingTechBlogs(passport: Passport): TechBlogList {
        val techBlogs = loadAll(passport.memberId)
        val meta = TechBlogListMeta(totalCount = techBlogs.size.toLong())
        return TechBlogList(meta, techBlogs)
    }

    private fun loadAll(memberId: Long): List<TechBlogSummary> {
        val subscriptions: List<SubscriptionInfo> =
            subscribedTechBlogReader.findAllSubscribedList(memberId)

        if (subscriptions.isEmpty()) return emptyList()

        val techBlogIds: List<Long> = subscriptions.map { it.techBlogId }
        val subscriptionMap: Map<Long, SubscriptionInfo> =
            subscriptions.associateBy { it.techBlogId }

        val cached = techBlogSummaryCache.mGet(techBlogIds)

        val missedIds = techBlogIds.filter { cached[it] == null }.distinct()
        val fetchedMap = if (missedIds.isEmpty()) {
            emptyMap()
        } else {
            val fetched = fetchTechBlogSummaries(missedIds)
            fetched.associateBy { it.id }.also {
                val cacheKeys = it.keys.map(techBlogSummaryCache::key)
                val warmupKey = WarmupCoordinator.Companion.msetKey("TechBlogSummaryCache", cacheKeys)
                warmupCoordinator.launchIfAbsent(warmupKey) {
                    techBlogSummaryCache.mSet(it)
                }
            }
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

    private fun fetchTechBlogSummaries(techBlogIds: List<Long>): List<TechBlogSummary> {
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

        val params = MapSqlParameterSource()
        techBlogIds.forEachIndexed { i, id -> params.addValue("id$i", id) }

        return jdbc.query(sql, params) { row, _ -> mapToTechBlogSummary(row) }
    }
}

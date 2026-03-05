package server.core.feature.techblog.query

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import server.core.infra.cache.TechBlogSubscriptionCache
import server.core.infra.cache.WarmupCoordinator

@Component
class SubscribedTechBlogReader(
    private val jdbc: NamedParameterJdbcTemplate,
    private val techBlogSubscriptionCache: TechBlogSubscriptionCache,
    private val warmupCoordinator: WarmupCoordinator,
) {

    fun findSubscribedMap(
        memberId: Long,
        techBlogIds: List<Long>
    ): Map<Long, TechBlogSubscriptionInfo> {
        if (techBlogIds.isEmpty()) return emptyMap()

        val allList = loadAll(memberId)
        val allMap = allList.associateBy { it.techBlogId }

        val result = LinkedHashMap<Long, TechBlogSubscriptionInfo>(techBlogIds.size)
        techBlogIds.forEach { id ->
            val info = allMap[id]
            if (info != null) result[id] = info
        }
        return result
    }

    fun findAllSubscribedList(memberId: Long): List<TechBlogSubscriptionInfo> {
        return loadAll(memberId)
    }

    private fun loadAll(memberId: Long): List<TechBlogSubscriptionInfo> {
        techBlogSubscriptionCache.get(memberId)?.let { return it }

        val sql = """
                SELECT
                    s.tech_blog_id AS tech_blog_id,
                    COALESCE(s.notification_enabled, 0) AS notification_enabled
                FROM tech_blog_subscription s
                WHERE s.member_id = :memberId
            """.trimIndent()

        val subscriptions: List<TechBlogSubscriptionInfo> = jdbc.query(sql, mapOf("memberId" to memberId)) { row, _ ->
                TechBlogSubscriptionInfo(
                    techBlogId = row.getLong("tech_blog_id"),
                    subscribed = true,
                    notificationEnabled = row.getInt("notification_enabled") == 1,
                )
            }

        val warmupKey = techBlogSubscriptionCache.versionKey(memberId)
        warmupCoordinator.launchIfAbsent(warmupKey) {
            techBlogSubscriptionCache.set(memberId, subscriptions)
        }

        return subscriptions
    }

    fun findById(
        memberId: Long,
        techBlogId: Long
    ): TechBlogSubscriptionInfo? {
        val all = loadAll(memberId)
        return all.firstOrNull { it.techBlogId == techBlogId }
    }
}

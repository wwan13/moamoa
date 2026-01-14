package server.feature.techblog.query

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Component
import server.infra.cache.TechBlogSubscriptionCache

@Component
class SubscribedTechBlogReader(
    private val databaseClient: DatabaseClient,
    private val techBlogSubscriptionCache: TechBlogSubscriptionCache,
    private val cacheWarmupScope: CoroutineScope,
) {

    suspend fun findSubscribedMap(
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

    suspend fun findAllSubscribedList(memberId: Long): List<TechBlogSubscriptionInfo> {
        return loadAll(memberId)
    }

    private suspend fun loadAll(memberId: Long): List<TechBlogSubscriptionInfo> {
        techBlogSubscriptionCache.get(memberId)?.let { return it }

        val sql = """
                SELECT
                    s.tech_blog_id AS tech_blog_id,
                    COALESCE(s.notification_enabled, 0) AS notification_enabled
                FROM tech_blog_subscription s
                WHERE s.member_id = :memberId
            """.trimIndent()

        return databaseClient.sql(sql)
            .bind("memberId", memberId)
            .map { row, _ ->
                TechBlogSubscriptionInfo(
                    techBlogId = row.get("tech_blog_id", Long::class.java) ?: 0L,
                    subscribed = true,
                    notificationEnabled = (row.get("notification_enabled", Int::class.java) ?: 0) == 1,
                )
            }
            .all()
            .asFlow()
            .toList()
            .also {
                cacheWarmupScope.launch {
                    techBlogSubscriptionCache.set(memberId, it)
                }
            }
    }
}
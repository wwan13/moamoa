package server.feature.techblog.query

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import server.infra.cache.TechBlogListCache
import server.infra.cache.WarmupCoordinator
import server.security.Passport

@Service
class TechBlogQueryService(
    private val jdbc: NamedParameterJdbcTemplate,
    private val techBlogListCache: TechBlogListCache,
    private val techBlogStatsReader: TechBlogStatsReader,
    private val subscribedTechBlogReader: SubscribedTechBlogReader,
    private val warmupCoordinator: WarmupCoordinator,
) {

    fun findAll(
        passport: Passport?,
        conditions: TechBlogQueryConditions
    ): TechBlogList {
        val base = loadBaseList(conditions.query)

        val meta = TechBlogListMeta(totalCount = base.size.toLong())
        if (base.isEmpty()) return TechBlogList(meta, emptyList())

        val ids = base.map { it.id }

        val statsMap = techBlogStatsReader.findTechBlogStatsMap(ids)
        val subscribedTechBlogMap = if (passport == null) emptyMap()
        else subscribedTechBlogReader.findSubscribedMap(passport.memberId, ids)

        val result = base.map { techBlog ->
            val stats = statsMap[techBlog.id]
            val subscriptionInfo = subscribedTechBlogMap[techBlog.id]

            techBlog.copy(
                subscriptionCount = stats?.subscriptionCount ?: 0L,
                postCount = stats?.postCount ?: 0L,
                subscribed = subscriptionInfo?.subscribed ?: false,
                notificationEnabled = subscriptionInfo?.notificationEnabled ?: false,
            )
        }

        return TechBlogList(meta, result)
    }

    private fun loadBaseList(
        query: String? = null
    ): List<TechBlogSummary> {
        if (query == null) {
            techBlogListCache.get()?.let { return it }
        }

        val whereClause = if (query != null) {
            """
            WHERE t.title LIKE :keyword
               OR t.blog_url LIKE :keyword
               OR t.tech_blog_key LIKE :keyword
        """.trimIndent()
        } else ""

        val sql = """
            SELECT
                t.id            AS tech_blog_id,
                t.title         AS tech_blog_title,
                t.icon          AS tech_blog_icon,
                t.blog_url      AS tech_blog_url,
                t.tech_blog_key AS tech_blog_key
            FROM tech_blog t
            $whereClause
            ORDER BY t.title ASC
        """.trimIndent()

        val params = MapSqlParameterSource()
        if (query != null) params.addValue("keyword", "%$query%")

        val list: List<TechBlogSummary> = jdbc.query(sql, params) { row, _ ->
                TechBlogSummary(
                    id = row.getLong("tech_blog_id"),
                    title = row.getString("tech_blog_title") ?: "",
                    icon = row.getString("tech_blog_icon") ?: "",
                    blogUrl = row.getString("tech_blog_url") ?: "",
                    key = row.getString("tech_blog_key") ?: "",
                    subscriptionCount = 0L,
                    postCount = 0L,
                    subscribed = false,
                    notificationEnabled = false,
                )
            }

        if (query == null) {
            warmupCoordinator.launchIfAbsent(techBlogListCache.key) {
                techBlogListCache.set(list)
            }
        }

        return list
    }

    fun findById(passport: Passport?, techBlogId: Long): TechBlogSummary {
        val base = loadBaseById(techBlogId)
            ?: throw IllegalStateException("존재하지 않는 기술블로그 입니다.")

        val stats = techBlogStatsReader.findById(base.id)
        val subInfo = if (passport == null) null
        else subscribedTechBlogReader.findById(passport.memberId, base.id)

        return base.copy(
            subscriptionCount = stats?.subscriptionCount ?: 0L,
            postCount = stats?.postCount ?: 0L,
            subscribed = subInfo?.subscribed ?: false,
            notificationEnabled = subInfo?.notificationEnabled ?: false,
        )
    }

    private fun loadBaseById(techBlogId: Long): TechBlogSummary? {
        val sql = """
            SELECT
                t.id            AS tech_blog_id,
                t.title         AS tech_blog_title,
                t.icon          AS tech_blog_icon,
                t.blog_url      AS tech_blog_url,
                t.tech_blog_key AS tech_blog_key
            FROM tech_blog t
            WHERE t.id = :id
            LIMIT 1
        """.trimIndent()

        return jdbc.query(sql, mapOf("id" to techBlogId)) { row, _ ->
                TechBlogSummary(
                    id = row.getLong("tech_blog_id"),
                    title = row.getString("tech_blog_title") ?: "",
                    icon = row.getString("tech_blog_icon") ?: "",
                    blogUrl = row.getString("tech_blog_url") ?: "",
                    key = row.getString("tech_blog_key") ?: "",
                    subscriptionCount = 0L,
                    postCount = 0L,
                    subscribed = false,
                    notificationEnabled = false,
                )
            }
            .firstOrNull()
    }
}

package server.feature.techblog.query

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Service
import server.infra.cache.TechBlogListCache
import server.security.Passport

@Service
class TechBlogQueryService(
    private val databaseClient: DatabaseClient,
    private val techBlogListCache: TechBlogListCache,
    private val techBlogStatsReader: TechBlogStatsReader,
    private val subscribedTechBlogReader: SubscribedTechBlogReader,
    private val cacheWarmupScope: CoroutineScope,
) {

    suspend fun findAll(
        passport: Passport?,
        conditions: TechBlogQueryConditions
    ): TechBlogList = coroutineScope {
        val baseDeferred = async { loadBaseList(conditions.query) }
        val base = baseDeferred.await()

        val meta = TechBlogListMeta(totalCount = base.size.toLong())
        if (base.isEmpty()) return@coroutineScope TechBlogList(meta, emptyList())

        val ids = base.map { it.id }

        val statsDeferred = async { techBlogStatsReader.findTechBlogStatsMap(ids) }

        val subscribedTechBlogMapDeferred = async {
            if (passport == null) emptyMap()
            else subscribedTechBlogReader.findSubscribedMap(passport.memberId, ids)
        }

        val statsMap = statsDeferred.await()
        val subscribedTechBlogMap = subscribedTechBlogMapDeferred.await()

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

        TechBlogList(meta, result)
    }

    private suspend fun loadBaseList(
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

        var spec = databaseClient.sql(sql)

        if (query != null) {
            spec = spec.bind("keyword", "%$query%")
        }

        return spec
            .map { row, _ ->
                TechBlogSummary(
                    id = row.get("tech_blog_id", Long::class.java) ?: 0L,
                    title = row.get("tech_blog_title", String::class.java).orEmpty(),
                    icon = row.get("tech_blog_icon", String::class.java).orEmpty(),
                    blogUrl = row.get("tech_blog_url", String::class.java).orEmpty(),
                    key = row.get("tech_blog_key", String::class.java).orEmpty(),
                    subscriptionCount = 0L,
                    postCount = 0L,
                    subscribed = false,
                    notificationEnabled = false,
                )
            }
            .all()
            .asFlow()
            .toList()
            .also { list ->
                if (query == null) {
                    cacheWarmupScope.launch { techBlogListCache.set(list) }
                }
            }
    }

    suspend fun findById(passport: Passport?, techBlogId: Long): TechBlogSummary = coroutineScope {
        val baseDeferred = async { loadBaseById(techBlogId) }
        val base = baseDeferred.await()
            ?: throw IllegalStateException("존재하지 않는 기술블로그 입니다.")

        val statsDeferred = async { techBlogStatsReader.findById(base.id) }
        val subInfoDeferred = async {
            if (passport == null) null
            else subscribedTechBlogReader.findById(passport.memberId, base.id)
        }

        val stats = statsDeferred.await()
        val subInfo = subInfoDeferred.await()

        base.copy(
            subscriptionCount = stats?.subscriptionCount ?: 0L,
            postCount = stats?.postCount ?: 0L,
            subscribed = subInfo?.subscribed ?: false,
            notificationEnabled = subInfo?.notificationEnabled ?: false,
        )
    }

    private suspend fun loadBaseById(techBlogId: Long): TechBlogSummary? {
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

        return databaseClient.sql(sql)
            .bind("id", techBlogId)
            .map { row, _ ->
                TechBlogSummary(
                    id = row.get("tech_blog_id", Long::class.java) ?: 0L,
                    title = row.get("tech_blog_title", String::class.java).orEmpty(),
                    icon = row.get("tech_blog_icon", String::class.java).orEmpty(),
                    blogUrl = row.get("tech_blog_url", String::class.java).orEmpty(),
                    key = row.get("tech_blog_key", String::class.java).orEmpty(),
                    subscriptionCount = 0L,
                    postCount = 0L,
                    subscribed = false,
                    notificationEnabled = false,
                )
            }
            .one()
            .asFlow()
            .toList()
            .firstOrNull()
    }
}
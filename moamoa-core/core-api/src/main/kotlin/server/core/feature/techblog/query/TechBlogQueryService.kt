package server.core.feature.techblog.query

import com.linecorp.kotlinjdsl.dsl.jpql.*
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Service
import server.core.feature.post.domain.Post
import server.core.feature.techblog.domain.TechBlog
import server.core.feature.techblog.infra.TechBlogListCache
import server.core.global.security.Passport
import server.core.infra.cache.WarmupCoordinator
import server.core.support.query.createJdslQuery

@Service
class TechBlogQueryService(
    @PersistenceContext
    private val entityManager: EntityManager,
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

        val list = findAllTechBlogs(query)

        if (query == null) {
            warmupCoordinator.launchIfAbsent(techBlogListCache.key) {
                techBlogListCache.set(list)
            }
        }

        return list
    }

    fun findById(passport: Passport?, techBlogId: Long): TechBlogSummary {
        val base = findTechBlogById(techBlogId)
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

    private fun findAllTechBlogs(query: String?): List<TechBlogSummary> {
        val keyword = query?.takeIf { it.isNotBlank() }?.let { "%$it%" }

        return entityManager
            .createJdslQuery(
                query = findAllTechBlogsQuery(keyword),
                resultClass = TechBlogSummary::class.java,
                offset = 0,
                limit = Int.MAX_VALUE,
            )
            .resultList
    }

    private fun findTechBlogById(techBlogId: Long): TechBlogSummary? {
        val blog = entityManager
            .createJdslQuery(
                query = findTechBlogByIdQuery(techBlogId),
                resultClass = TechBlogSummary::class.java,
                offset = 0,
                limit = 1,
            )
            .resultList
            .firstOrNull() ?: return null

        val postCount = findPostCountMap(listOf(techBlogId))[techBlogId] ?: 0L
        return blog.copy(postCount = postCount)
    }

    private fun findPostCountMap(techBlogIds: List<Long>): Map<Long, Long> {
        if (techBlogIds.isEmpty()) return emptyMap()

        return entityManager
            .createJdslQuery(
                query = findPostCountMapQuery(techBlogIds),
                resultClass = TechBlogStats::class.java,
                offset = 0,
                limit = Int.MAX_VALUE,
            )
            .resultList
            .associate { it.techBlogId to it.postCount }
    }

    private fun findAllTechBlogsQuery(keyword: String?) = jpql {
        selectBaseTechBlogSummary()
            .from(
                entity(TechBlog::class)
            )
            .whereAnd(
                keyword?.let {
                    or(
                        path(TechBlog::title).like(it),
                        path(TechBlog::blogUrl).like(it),
                        path(TechBlog::key).like(it),
                    )
                }
            )
            .orderBy(path(TechBlog::title).asc())
    }

    private fun findTechBlogByIdQuery(techBlogId: Long) = jpql {
        selectBaseTechBlogSummary()
            .from(
                entity(TechBlog::class)
            )
            .where(
                path(TechBlog::id).equal(techBlogId)
            )
    }

    private fun findPostCountMapQuery(techBlogIds: List<Long>) = jpql {
        selectNew<TechBlogStats>(
            path(Post::techBlogId),
            longLiteral(0L),
            count(path(Post::id)),
        )
            .from(
                entity(Post::class)
            )
            .where(
                path(Post::techBlogId).`in`(techBlogIds)
            )
            .groupBy(
                path(Post::techBlogId)
            )
    }
}

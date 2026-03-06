package server.core.feature.techblog.query

import com.linecorp.kotlinjdsl.dsl.jpql.*
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Component
import server.core.feature.post.domain.Post
import server.core.feature.techblog.domain.TechBlog
import server.core.feature.techblog.infra.TechBlogSummaryCache
import server.core.infra.cache.WarmupCoordinator
import server.core.support.query.createJdslQuery

@Component
class TechBlogStatsReader(
    @PersistenceContext
    private val entityManager: EntityManager,
    private val techBlogSummaryCache: TechBlogSummaryCache,
    private val warmupCoordinator: WarmupCoordinator,
) {

    fun findTechBlogStatsMap(techBlogIds: List<Long>): Map<Long, TechBlogStats> {
        if (techBlogIds.isEmpty()) return emptyMap()

        val cached = techBlogSummaryCache.mGet(techBlogIds)
        val missedIds = techBlogIds.filter { cached[it] == null }

        val dbMap = if (missedIds.isNotEmpty()) {
            findTechBlogSummaryMap(missedIds)
        } else {
            emptyMap()
        }
        if (dbMap.isNotEmpty()) {
            val cacheKeys = dbMap.keys.map(techBlogSummaryCache::key)
            val warmupKey = WarmupCoordinator.Companion.msetKey("TechBlogSummaryCache", cacheKeys)
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

    fun findById(techBlogId: Long): TechBlogStats? {
        val cached = techBlogSummaryCache.get(techBlogId)
        if (cached != null) {
            return TechBlogStats(
                techBlogId = cached.id,
                subscriptionCount = cached.subscriptionCount,
                postCount = cached.postCount,
            )
        }

        val summary = findTechBlogSummaryMap(listOf(techBlogId))[techBlogId] ?: return null

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

    private fun findTechBlogSummaryMap(ids: List<Long>): Map<Long, TechBlogSummary> {
        if (ids.isEmpty()) return emptyMap()

        val rows = entityManager
            .createJdslQuery(
                query = findTechBlogSummaryMapQuery(ids),
                resultClass = TechBlogSummary::class.java,
                offset = 0,
                limit = Int.MAX_VALUE,
            )
            .resultList
        val postCountMap = findPostCountMap(ids)
        return rows
            .map { row -> row.copy(postCount = postCountMap[row.id] ?: 0L) }
            .associateBy { it.id }
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

    private fun findTechBlogSummaryMapQuery(ids: List<Long>) = jpql {
        selectBaseTechBlogSummary()
            .from(
                entity(TechBlog::class)
            )
            .where(
                path(TechBlog::id).`in`(ids)
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

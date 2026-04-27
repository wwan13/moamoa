package server.core.feature.techblog.query

import com.linecorp.kotlinjdsl.dsl.jpql.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import server.core.feature.post.domain.Post
import server.core.feature.techblog.domain.TechBlog
import server.core.feature.techblog.infra.TechBlogSummaryCache
import server.core.global.jdsl.JdslExecutor
import server.core.global.security.Passport
import server.core.infra.cache.WarmupCoordinator
import kotlin.collections.emptyMap
import kotlin.collections.forEach

@Service
class SubscribedTechBlogQueryService(
    private val jdslExecutor: JdslExecutor,
    private val subscribedTechBlogReader: SubscribedTechBlogReader,
    private val techBlogSummaryCache: TechBlogSummaryCache,
    private val warmupCoordinator: WarmupCoordinator,
) {

    @Transactional(readOnly = true)
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
            findTechBlogSummaryMap(missedIds).also {
                val cacheKeys = it.keys.map(techBlogSummaryCache::key)
                val warmupKey = WarmupCoordinator.msetKey("TechBlogSummaryCache", cacheKeys)
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

    private fun findTechBlogSummaryMap(ids: List<Long>): Map<Long, TechBlogSummary> {
        if (ids.isEmpty()) return emptyMap()

        val rows = jdslExecutor
            .createQuery(
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

        return jdslExecutor
            .createQuery(
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

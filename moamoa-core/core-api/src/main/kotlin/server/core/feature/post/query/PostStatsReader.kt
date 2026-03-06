package server.core.feature.post.query

import com.linecorp.kotlinjdsl.dsl.jpql.*
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Component
import server.core.feature.post.domain.Post
import server.core.feature.post.infra.PostStatsCache
import server.core.infra.cache.WarmupCoordinator
import server.core.support.query.createJdslQuery

@Component
class PostStatsReader(
    @PersistenceContext
    private val entityManager: EntityManager,
    private val postStatsCache: PostStatsCache,
    private val warmupCoordinator: WarmupCoordinator,
) {

    fun findPostStatsMap(postIds: List<Long>): Map<Long, PostStats> {
        if (postIds.isEmpty()) return emptyMap()

        val cachedMap = postStatsCache.mGet(postIds)
        val missedIds = postIds.filter { cachedMap[it] == null }

        val dbMap = if (missedIds.isNotEmpty()) {
            queryPostStatsMap(missedIds)
        } else {
            emptyMap()
        }

        if (dbMap.isNotEmpty()) {
            val cacheKeys = dbMap.keys.map(postStatsCache::key)
            val warmupKey = WarmupCoordinator.Companion.msetKey("PostStatsCache", cacheKeys)
            warmupCoordinator.launchIfAbsent(warmupKey) {
                postStatsCache.mSet(dbMap)
            }
        }

        val result = LinkedHashMap<Long, PostStats>(postIds.size)
        postIds.forEach { id ->
            val v = cachedMap[id] ?: dbMap[id]
            if (v != null) result[id] = v
        }
        return result
    }

    private fun queryPostStatsMap(postIds: List<Long>): Map<Long, PostStats> {
        if (postIds.isEmpty()) return emptyMap()

        val jpqlQuery = jpql {
            selectNew<PostStats>(
                path(Post::id),
                path(Post::viewCount),
                path(Post::bookmarkCount),
            )
                .from(
                    entity(Post::class)
                )
                .where(
                    path(Post::id).`in`(postIds)
                )
        }

        return entityManager
            .createJdslQuery(
                query = jpqlQuery,
                resultClass = PostStats::class.java,
                offset = 0,
                limit = Int.MAX_VALUE,
            )
            .resultList
            .associateBy { it.postId }
    }
}

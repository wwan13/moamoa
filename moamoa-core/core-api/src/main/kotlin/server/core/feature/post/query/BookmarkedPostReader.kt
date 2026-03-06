package server.core.feature.post.query

import com.linecorp.kotlinjdsl.dsl.jpql.*
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Component
import server.core.feature.bookmark.domain.Bookmark
import server.core.feature.post.infra.BookmarkedAllPostIdSetCache
import server.core.infra.cache.WarmupCoordinator
import server.core.support.query.createJdslQuery

@Component
class BookmarkedPostReader(
    @PersistenceContext
    private val entityManager: EntityManager,
    private val bookmarkedAllPostIdSetCache: BookmarkedAllPostIdSetCache,
    private val warmupCoordinator: WarmupCoordinator,
) {

    fun findBookmarkedPostIdSet(
        memberId: Long,
        postIds: List<Long>,
    ): Set<Long> {
        if (postIds.isEmpty()) return emptySet()

        val cachedAll = bookmarkedAllPostIdSetCache.get(memberId)
        if (cachedAll != null) {
            return postIds.asSequence().filter { cachedAll.contains(it) }.toSet()
        }

        return queryBookmarkedPostIdSet(memberId, postIds).also {
            val warmupKey = bookmarkedAllPostIdSetCache.versionKey(memberId)
            warmupCoordinator.launchIfAbsent(warmupKey) {
                warmUpAllBookmarkedSet(memberId)
            }
        }
    }

    private fun warmUpAllBookmarkedSet(memberId: Long) {
        if (bookmarkedAllPostIdSetCache.get(memberId) != null) return

        val allIds = queryAllBookmarkedPostIds(memberId)
        bookmarkedAllPostIdSetCache.set(memberId, allIds)
    }

    private fun queryAllBookmarkedPostIds(memberId: Long): Set<Long> {
        val jpqlQuery = jpql {
            select(path(Bookmark::postId))
                .from(
                    entity(Bookmark::class)
                )
                .where(
                    path(Bookmark::memberId).equal(memberId)
                )
        }

        return entityManager
            .createJdslQuery(
                query = jpqlQuery,
                resultClass = Long::class.javaObjectType,
                offset = 0,
                limit = Int.MAX_VALUE,
            )
            .resultList
            .toSet()
    }

    private fun queryBookmarkedPostIdSet(memberId: Long, postIds: List<Long>): Set<Long> {
        if (postIds.isEmpty()) return emptySet()

        val jpqlQuery = jpql {
            select(path(Bookmark::postId))
                .from(
                    entity(Bookmark::class)
                )
                .whereAnd(
                    path(Bookmark::memberId).equal(memberId),
                    path(Bookmark::postId).`in`(postIds),
                )
        }

        return entityManager
            .createJdslQuery(
                query = jpqlQuery,
                resultClass = Long::class.javaObjectType,
                offset = 0,
                limit = Int.MAX_VALUE,
            )
            .resultList
            .toSet()
    }
}

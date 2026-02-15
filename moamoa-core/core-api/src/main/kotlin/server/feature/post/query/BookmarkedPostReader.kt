package server.feature.post.query

import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.reactive.asFlow
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Component
import server.infra.cache.BookmarkedAllPostIdSetCache
import server.infra.cache.WarmupCoordinator

@Component
class BookmarkedPostReader(
    private val databaseClient: DatabaseClient,
    private val bookmarkedAllPostIdSetCache: BookmarkedAllPostIdSetCache,
    private val warmupCoordinator: WarmupCoordinator,
) {

    suspend fun findBookmarkedPostIdSet(
        memberId: Long,
        postIds: List<Long>,
    ): Set<Long> {
        if (postIds.isEmpty()) return emptySet()

        val cachedAll = bookmarkedAllPostIdSetCache.get(memberId)
        if (cachedAll != null) {
            return postIds.asSequence().filter { cachedAll.contains(it) }.toSet()
        }

        return fetchBookmarkedPostIdSetByIn(memberId, postIds).also {
            val warmupKey = bookmarkedAllPostIdSetCache.versionKey(memberId)
            warmupCoordinator.launchIfAbsent(warmupKey) {
                warmUpAllBookmarkedSet(memberId)
            }
        }
    }

    private suspend fun warmUpAllBookmarkedSet(memberId: Long) {
        if (bookmarkedAllPostIdSetCache.get(memberId) != null) return

        val sql = """
            SELECT pb.post_id AS post_id
            FROM post_bookmark pb
            WHERE pb.member_id = :memberId
        """.trimIndent()

        val allIds = databaseClient.sql(sql)
            .bind("memberId", memberId)
            .map { row, _ -> row.get("post_id", Long::class.java) ?: 0L }
            .all()
            .asFlow()
            .toSet()

        bookmarkedAllPostIdSetCache.set(memberId, allIds)
    }

    private suspend fun fetchBookmarkedPostIdSetByIn(
        memberId: Long,
        postIds: List<Long>,
    ): Set<Long> {
        val placeholders = postIds.indices.joinToString(",") { ":id$it" }

        val sql = """
            SELECT pb.post_id AS post_id
            FROM post_bookmark pb
            WHERE pb.member_id = :memberId
              AND pb.post_id IN ($placeholders)
        """.trimIndent()

        var spec = databaseClient.sql(sql).bind("memberId", memberId)
        postIds.forEachIndexed { idx, id -> spec = spec.bind("id$idx", id) }

        return spec
            .map { row, _ -> row.get("post_id", Long::class.java) ?: 0L }
            .all()
            .asFlow()
            .toSet()
    }
}

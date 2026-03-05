package server.core.feature.post.query

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import server.core.infra.cache.BookmarkedAllPostIdSetCache
import server.core.infra.cache.WarmupCoordinator

@Component
class BookmarkedPostReader(
    private val jdbc: NamedParameterJdbcTemplate,
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

        return fetchBookmarkedPostIdSetByIn(memberId, postIds).also {
            val warmupKey = bookmarkedAllPostIdSetCache.versionKey(memberId)
            warmupCoordinator.launchIfAbsent(warmupKey) {
                warmUpAllBookmarkedSet(memberId)
            }
        }
    }

    private fun warmUpAllBookmarkedSet(memberId: Long) {
        if (bookmarkedAllPostIdSetCache.get(memberId) != null) return

        val sql = """
            SELECT pb.post_id AS post_id
            FROM bookmark pb
            WHERE pb.member_id = :memberId
        """.trimIndent()

        val allIds = jdbc.query(
            sql,
            mapOf("memberId" to memberId)
        ) { rs, _ -> rs.getLong("post_id") }.toSet()

        bookmarkedAllPostIdSetCache.set(memberId, allIds)
    }

    private fun fetchBookmarkedPostIdSetByIn(
        memberId: Long,
        postIds: List<Long>,
    ): Set<Long> {
        val placeholders = postIds.indices.joinToString(",") { ":id$it" }

        val sql = """
            SELECT pb.post_id AS post_id
            FROM bookmark pb
            WHERE pb.member_id = :memberId
              AND pb.post_id IN ($placeholders)
        """.trimIndent()

        val params = MapSqlParameterSource().addValue("memberId", memberId)
        postIds.forEachIndexed { idx, id -> params.addValue("id$idx", id) }

        return jdbc.query(sql, params) { rs, _ -> rs.getLong("post_id") }.toSet()
    }
}

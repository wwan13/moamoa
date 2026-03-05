package server.core.feature.post.query

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import server.core.infra.cache.BookmarkedPostListCache
import server.core.infra.cache.WarmupCoordinator
import server.core.global.security.Passport
import server.core.support.paging.Paging
import server.core.support.paging.calculateTotalPage

@Service
class BookmarkedPostQueryService(
    private val jdbc: NamedParameterJdbcTemplate,
    private val bookmarkedPostListCache: BookmarkedPostListCache,
    private val postStatsReader: PostStatsReader,
    private val warmupCoordinator: WarmupCoordinator,
) {

    fun findAllByConditions(
        conditions: PostQueryConditions,
        passport: Passport,
    ): PostList {

        val paging = Paging(
            size = conditions.size ?: 20,
            page = conditions.page ?: 1
        )

        val totalCount = countBookmarkedPosts(memberId = passport.memberId)
        val basePosts = loadPosts(memberId = passport.memberId, paging = paging)

        val meta = PostListMeta(
            page = paging.page,
            size = paging.size,
            totalCount = totalCount,
            totalPages = calculateTotalPage(totalCount, paging.size)
        )

        if (basePosts.isEmpty()) return PostList(meta, basePosts)

        val postIds = basePosts.map { it.id }

        val postStatsByPostId = postStatsReader.findPostStatsMap(postIds)

        val posts = basePosts.map { post ->
            post.copy(
                bookmarkCount = postStatsByPostId[post.id]?.bookmarkCount ?: post.bookmarkCount,
                viewCount = postStatsByPostId[post.id]?.viewCount ?: post.viewCount,
                isBookmarked = true,
            )
        }

        return PostList(meta, posts)
    }

    private fun loadPosts(memberId: Long, paging: Paging): List<PostSummary> {
        if (paging.page > 5) {
            return fetchBookmarkedBasePosts(paging, memberId)
        }

        val cached = bookmarkedPostListCache.get(memberId, paging.page)
        if (cached != null) return cached

        return fetchBookmarkedBasePosts(paging, memberId).also { posts ->
            val warmupKey = "${bookmarkedPostListCache.versionKey(memberId)}:PAGE:${paging.page}"
            warmupCoordinator.launchIfAbsent(warmupKey) {
                bookmarkedPostListCache.set(memberId, paging.page, posts)
            }
        }
    }

    private fun fetchBookmarkedBasePosts(
        paging: Paging,
        memberId: Long,
    ): List<PostSummary> {
        val offset = (paging.page - 1L) * paging.size

        val sql = """
            $POST_QUERY_BASE_SELECT,
                1 AS is_bookmarked
            FROM bookmark pb
            INNER JOIN post p ON p.id = pb.post_id
            INNER JOIN tech_blog t ON t.id = p.tech_blog_id
            WHERE pb.member_id = :memberId
            ORDER BY pb.created_at DESC
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("memberId", memberId)
            .addValue("limit", paging.size)
            .addValue("offset", offset)
        return jdbc.query(sql, params) { rs, _ -> mapToPostSummary(rs) }
    }

    private fun countBookmarkedPosts(memberId: Long): Long {
        val sql = """
            SELECT COUNT(*) AS cnt
            FROM bookmark pb
            WHERE pb.member_id = :memberId
        """.trimIndent()

        return jdbc.queryForObject(sql, mapOf("memberId" to memberId), Long::class.java) ?: 0L
    }
}

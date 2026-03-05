package server.core.feature.post.query

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import server.core.feature.post.infra.PostListCache
import server.core.infra.cache.WarmupCoordinator
import server.core.global.security.Passport
import server.core.support.paging.Paging
import server.core.support.paging.calculateTotalPage

@Service
class PostQueryService(
    private val jdbc: NamedParameterJdbcTemplate,
    private val postListCache: PostListCache,
    private val bookmarkedPostReader: BookmarkedPostReader,
    private val postStatsReader: PostStatsReader,
    private val warmupCoordinator: WarmupCoordinator,
) {

    fun findByConditions(
        conditions: PostQueryConditions,
        passport: Passport?
    ): PostList {

        val paging = Paging(
            size = conditions.size ?: 20,
            page = conditions.page ?: 1
        )

        val totalCount = countAll(conditions.query)
        val basePosts = loadPosts(paging, conditions.query)

        val meta = PostListMeta(
            page = paging.page,
            size = paging.size,
            totalCount = totalCount,
            totalPages = calculateTotalPage(totalCount, paging.size)
        )

        if (basePosts.isEmpty()) return PostList(meta, basePosts)

        val postIds = basePosts.map { it.id }

        val postStatsByPostId = postStatsReader.findPostStatsMap(postIds)
        val bookmarkedIdSet = if (passport == null) emptySet()
        else bookmarkedPostReader.findBookmarkedPostIdSet(
            memberId = passport.memberId,
            postIds = postIds
        )

        val posts = basePosts.map { post ->
            post.copy(
                bookmarkCount = postStatsByPostId[post.id]?.bookmarkCount ?: post.bookmarkCount,
                viewCount = postStatsByPostId[post.id]?.viewCount ?: post.viewCount,
                isBookmarked = bookmarkedIdSet.contains(post.id),
            )
        }

        return PostList(meta, posts)
    }

    private fun loadPosts(
        paging: Paging,
        query: String?
    ): List<PostSummary> {
        if (paging.page > 5 || !query.isNullOrBlank()) {
            return fetchBasePosts(paging, query)
        }

        val cached = postListCache.get(paging.page, paging.size)
        if (cached != null) return cached

        return fetchBasePosts(paging).also { posts ->
            val warmupKey = postListCache.key(paging.page, paging.size)
            warmupCoordinator.launchIfAbsent(warmupKey) {
                postListCache.set(paging.page, paging.size, posts)
            }
        }
    }

    private fun fetchBasePosts(
        paging: Paging,
        query: String? = null
    ): List<PostSummary> {
        val offset = (paging.page - 1L) * paging.size

        val whereClause = if (!query.isNullOrBlank()) {
            """
            WHERE
                p.title LIKE :keyword
                OR p.description LIKE :keyword
                OR EXISTS (
                    SELECT 1
                    FROM post_tag pt
                    INNER JOIN tag tg ON tg.id = pt.tag_id
                    WHERE pt.post_id = p.id
                      AND tg.title LIKE :keyword
                )
            """.trimIndent()
        } else {
            ""
        }

        val sql = """
            $POST_QUERY_BASE_SELECT,
                0 AS is_bookmarked
            FROM post p
            INNER JOIN tech_blog t ON t.id = p.tech_blog_id
            $whereClause
            ORDER BY p.published_at DESC
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("limit", paging.size)
            .addValue("offset", offset)
        if (query != null) params.addValue("keyword", "%$query%")

        return jdbc.query(sql, params) { rs, _ -> mapToPostSummary(rs) }
    }

    private fun countAll(query: String?): Long {
        val whereClause = if (query != null) {
            "WHERE title LIKE :keyword OR description LIKE :keyword"
        } else ""

        val sql = """
        SELECT COUNT(*) AS cnt
        FROM post
        $whereClause
    """.trimIndent()

        val params = MapSqlParameterSource()
        if (query != null) params.addValue("keyword", "%$query%")

        return jdbc.queryForObject(sql, params, Long::class.java) ?: 0L
    }
}

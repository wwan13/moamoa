package server.feature.post.query

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import server.infra.cache.TechBlogPostListCache
import server.infra.cache.WarmupCoordinator
import server.security.Passport
import support.paging.Paging
import support.paging.calculateTotalPage

@Service
class TechBlogPostQueryService(
    private val jdbc: NamedParameterJdbcTemplate,
    private val techBlogPostListCache: TechBlogPostListCache,
    private val bookmarkedPostReader: BookmarkedPostReader,
    private val postStatsReader: PostStatsReader,
    private val warmupCoordinator: WarmupCoordinator,
) {

    fun findAllByConditions(
        conditions: TechBlogPostQueryConditions,
        passport: Passport?,
    ): PostList {

        val paging = Paging(
            size = conditions.size ?: 20,
            page = conditions.page ?: 1
        )

        val totalCount = countByTechBlogKey(conditions.techBlogId)
        val basePosts = loadPosts(paging, conditions.techBlogId)

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
        techBlogId: Long,
    ): List<PostSummary> {
        if (paging.page > 5) {
            return fetchBasePostsByTechBlogKey(paging, techBlogId)
        }

        val cached = techBlogPostListCache.get(techBlogId, paging.page)
        if (cached != null) return cached

        return fetchBasePostsByTechBlogKey(paging, techBlogId).also { posts ->
            val warmupKey = techBlogPostListCache.key(techBlogId, paging.page)
            warmupCoordinator.launchIfAbsent(warmupKey) {
                techBlogPostListCache.set(techBlogId, paging.page, posts)
            }
        }
    }

    private fun fetchBasePostsByTechBlogKey(
        paging: Paging,
        techBlogId: Long,
    ): List<PostSummary> {
        val offset = (paging.page - 1L) * paging.size

        val sql = """
            $POST_QUERY_BASE_SELECT,
                0 AS is_bookmarked
            FROM post p
            INNER JOIN tech_blog t ON t.id = p.tech_blog_id
            WHERE t.id = :techBlogId
            ORDER BY p.published_at DESC
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("techBlogId", techBlogId)
            .addValue("limit", paging.size)
            .addValue("offset", offset)
        return jdbc.query(sql, params) { rs, _ -> mapToPostSummary(rs) }
    }

    private fun countByTechBlogKey(techBlogId: Long): Long {
        val sql = """
            SELECT COUNT(*) AS cnt
            FROM post p
            INNER JOIN tech_blog t ON t.id = p.tech_blog_id
            WHERE t.id = :techBlogId
        """.trimIndent()

        return jdbc.queryForObject(sql, mapOf("techBlogId" to techBlogId), Long::class.java) ?: 0L
    }
}

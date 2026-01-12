package server.feature.post.query

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Service
import server.infra.cache.TechBlogPostListCache
import server.security.Passport
import support.paging.Paging
import support.paging.calculateTotalPage

@Service
class TechBlogPostQueryService(
    private val databaseClient: DatabaseClient,
    private val techBlogPostListCache: TechBlogPostListCache,
    private val bookmarkedPostReader: BookmarkedPostReader
) {

    suspend fun findAllByConditions(
        conditions: TechBlogPostQueryConditions,
        passport: Passport?,
    ): PostList {
        val paging = Paging(
            size = conditions.size ?: 20,
            page = conditions.page ?: 1
        )

        val totalCount = countByTechBlogKey(conditions.techBlogKey)

        val meta = PostListMeta(
            page = paging.page,
            size = paging.size,
            totalCount = totalCount,
            totalPages = calculateTotalPage(totalCount, paging.size)
        )

        val basePosts = loadPosts(paging, conditions.techBlogKey)

        return if (passport != null && basePosts.isNotEmpty()) {
            val bookmarkedIds = bookmarkedPostReader.findBookmarkedPostIdSet(
                memberId = passport.memberId,
                postIds = basePosts.map { it.id })
            val posts = basePosts.map {
                it.copy(isBookmarked = bookmarkedIds.contains(it.id))
            }
            PostList(meta, posts)
        } else {
            PostList(meta, basePosts)
        }
    }

    private suspend fun loadPosts(
        paging: Paging,
        techBlogKey: String,
    ): List<PostSummary> {
        if (paging.page > 5) {
            return findAllByTechBlogKey(paging, techBlogKey).toList()
        }

        val fromCache = techBlogPostListCache.get(techBlogKey, paging.page)
        if (fromCache != null) {
            return fromCache
        }

        return findAllByTechBlogKey(paging, techBlogKey).toList().also {
            techBlogPostListCache.set(techBlogKey, paging.page, it)
        }
    }

    private suspend fun findAllByTechBlogKey(
        paging: Paging,
        techBlogKey: String,
    ): Flow<PostSummary> {
        val offset = (paging.page - 1L) * paging.size

        val sql = """
            $POST_QUERY_BASE_SELECT,
                0 AS is_bookmarked
            FROM post p
            INNER JOIN tech_blog t ON t.id = p.tech_blog_id
            WHERE t.tech_blog_key = :techBlogKey
            ORDER BY p.published_at DESC
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return databaseClient.sql(sql)
            .bind("techBlogKey", techBlogKey)
            .bind("limit", paging.size)
            .bind("offset", offset)
            .map { row, _ -> mapToPostSummary(row) }
            .all()
            .asFlow()
    }

    private suspend fun countByTechBlogKey(techBlogKey: String): Long {
        val sql = """
            SELECT COUNT(*) AS cnt
            FROM post p
            INNER JOIN tech_blog t ON t.id = p.tech_blog_id
            WHERE t.tech_blog_key = :techBlogKey
        """.trimIndent()

        return databaseClient.sql(sql)
            .bind("techBlogKey", techBlogKey)
            .map { row, _ -> row.get("cnt", Long::class.java) ?: 0L }
            .one()
            .awaitSingle()
    }
}
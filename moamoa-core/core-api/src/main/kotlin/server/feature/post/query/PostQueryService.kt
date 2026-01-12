package server.feature.post.query

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Service
import server.infra.cache.PostListCache
import server.security.Passport
import support.paging.Paging
import support.paging.calculateTotalPage

@Service
class PostQueryService(
    private val databaseClient: DatabaseClient,
    private val postListCache: PostListCache,
    private val bookmarkedPostReader: BookmarkedPostReader
) {

    suspend fun findByConditions(
        conditions: PostQueryConditions,
        passport: Passport?
    ): PostList {
        val paging = Paging(
            size = conditions.size ?: 20,
            page = conditions.page ?: 1
        )

        val totalCount = countAll()

        val meta = PostListMeta(
            page = paging.page,
            size = paging.size,
            totalCount = totalCount,
            totalPages = calculateTotalPage(totalCount, paging.size)
        )

        val basePosts = loadPosts(paging)

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

    private suspend fun loadPosts(paging: Paging): List<PostSummary> {
        if (paging.page > 5) {
            return findAll(paging).toList()
        }

        val fromCache = postListCache.get(paging.page)
        if (fromCache != null) {
            return fromCache
        }

        return findAll(paging).toList().also {
            postListCache.set(paging.page, it)
        }
    }

    private suspend fun findAll(paging: Paging): Flow<PostSummary> {
        val offset = (paging.page - 1L) * paging.size

        val sql = """
            $POST_QUERY_BASE_SELECT,
                0 AS is_bookmarked
            FROM post p
            INNER JOIN tech_blog t ON t.id = p.tech_blog_id
            ORDER BY p.published_at DESC
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return databaseClient.sql(sql)
            .bind("limit", paging.size)
            .bind("offset", offset)
            .map { row, _ -> mapToPostSummary(row) }
            .all()
            .asFlow()
    }

    private suspend fun countAll(): Long {
        val sql = """
            SELECT COUNT(*) AS cnt
            FROM post
        """.trimIndent()

        return databaseClient.sql(sql)
            .map { row, _ -> row.get("cnt", Long::class.java) ?: 0L }
            .one()
            .awaitSingle()
    }
}
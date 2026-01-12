package server.feature.post.query

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Service
import server.infra.cache.BookmarkedPostListCache
import server.security.Passport
import support.paging.Paging
import support.paging.calculateTotalPage

@Service
class BookmarkedPostQueryService(
    private val databaseClient: DatabaseClient,
    private val bookmarkedPostListCache: BookmarkedPostListCache,
    private val postBookmarkCountReader: PostBookmarkCountReader,
) {

    suspend fun findAllByConditions(
        conditions: PostQueryConditions,
        passport: Passport,
    ): PostList = coroutineScope {

        val paging = Paging(
            size = conditions.size ?: 20,
            page = conditions.page ?: 1
        )

        val totalCountDeferred = async { countBookmarkedPosts(memberId = passport.memberId) }
        val basePostsDeferred = async { loadPosts(memberId = passport.memberId, paging = paging) }

        val totalCount = totalCountDeferred.await()
        val basePosts = basePostsDeferred.await()

        val meta = PostListMeta(
            page = paging.page,
            size = paging.size,
            totalCount = totalCount,
            totalPages = calculateTotalPage(totalCount, paging.size)
        )

        if (basePosts.isEmpty()) return@coroutineScope PostList(meta, basePosts)

        val postIds = basePosts.map { it.id }

        val bookmarkCountMapDeferred = async {
            postBookmarkCountReader.findBookmarkCountMap(postIds)
        }

        val bookmarkCountMap = bookmarkCountMapDeferred.await()

        val posts = basePosts.map { post ->
            post.copy(
                bookmarkCount = bookmarkCountMap[post.id] ?: post.bookmarkCount,
                isBookmarked = true,
            )
        }

        PostList(meta, posts)
    }

    private suspend fun loadPosts(memberId: Long, paging: Paging): List<PostSummary> {
        if (paging.page > 5) {
            return fetchBookmarkedBasePosts(paging, memberId).toList()
        }

        val cached = bookmarkedPostListCache.get(memberId, paging.page)
        if (cached != null) return cached

        return fetchBookmarkedBasePosts(paging, memberId).toList().also {
            bookmarkedPostListCache.set(memberId, paging.page, it)
        }
    }

    private suspend fun fetchBookmarkedBasePosts(
        paging: Paging,
        memberId: Long,
    ): Flow<PostSummary> {
        val offset = (paging.page - 1L) * paging.size

        val sql = """
            $POST_QUERY_BASE_SELECT,
                1 AS is_bookmarked
            FROM post_bookmark pb
            INNER JOIN post p ON p.id = pb.post_id
            INNER JOIN tech_blog t ON t.id = p.tech_blog_id
            WHERE pb.member_id = :memberId
            ORDER BY pb.created_at DESC
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return databaseClient.sql(sql)
            .bind("memberId", memberId)
            .bind("limit", paging.size)
            .bind("offset", offset)
            .map { row, _ -> mapToPostSummary(row) }
            .all()
            .asFlow()
    }

    private suspend fun countBookmarkedPosts(memberId: Long): Long {
        val sql = """
            SELECT COUNT(*) AS cnt
            FROM post_bookmark pb
            WHERE pb.member_id = :memberId
        """.trimIndent()

        return databaseClient.sql(sql)
            .bind("memberId", memberId)
            .map { row, _ -> row.get("cnt", Long::class.java) ?: 0L }
            .one()
            .awaitSingle()
    }
}
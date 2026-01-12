package server.feature.post.query

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Service
import server.infra.cache.SubscribedPostListCache
import server.security.Passport
import support.paging.Paging
import support.paging.calculateTotalPage

@Service
class SubscribedPostQueryService(
    private val databaseClient: DatabaseClient,
    private val subscribedPostListCache: SubscribedPostListCache,
    private val bookmarkedPostReader: BookmarkedPostReader,
    private val postStatsReader: PostStatsReader,
) {

    suspend fun findAllByConditions(
        conditions: PostQueryConditions,
        passport: Passport,
    ): PostList = coroutineScope {

        val paging = Paging(
            size = conditions.size ?: 20,
            page = conditions.page ?: 1
        )

        val totalCountDeferred = async { countSubscribingPosts(memberId = passport.memberId) }
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
            postStatsReader.findBookmarkCountMap(postIds)
        }

        val bookmarkedIdSetDeferred = async {
            bookmarkedPostReader.findBookmarkedPostIdSet(
                memberId = passport.memberId,
                postIds = postIds
            )
        }

        val bookmarkedIdSet = bookmarkedIdSetDeferred.await()
        val postStatsByPostId = bookmarkCountMapDeferred.await()

        val posts = basePosts.map { post ->
            post.copy(
                bookmarkCount = postStatsByPostId[post.id]?.bookmarkCount ?: post.bookmarkCount,
                viewCount = postStatsByPostId[post.id]?.viewCount ?: post.viewCount,
                isBookmarked = bookmarkedIdSet.contains(post.id),
            )
        }

        PostList(meta, posts)
    }

    private suspend fun loadPosts(memberId: Long, paging: Paging): List<PostSummary> {
        if (paging.page > 5) {
            return fetchSubscribingBasePosts(paging, memberId).toList()
        }

        val cached = subscribedPostListCache.get(memberId, paging.page)
        if (cached != null) return cached

        return fetchSubscribingBasePosts(paging, memberId).toList().also {
            subscribedPostListCache.set(memberId, paging.page, it)
        }
    }

    private suspend fun fetchSubscribingBasePosts(
        paging: Paging,
        memberId: Long,
    ): Flow<PostSummary> {
        val offset = (paging.page - 1L) * paging.size

        val sql = """
            $POST_QUERY_BASE_SELECT,
                0 AS is_bookmarked
            FROM tech_blog_subscription s
            INNER JOIN tech_blog t ON t.id = s.tech_blog_id
            INNER JOIN post p ON p.tech_blog_id = t.id
            WHERE s.member_id = :memberId
            ORDER BY p.published_at DESC
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

    private suspend fun countSubscribingPosts(memberId: Long): Long {
        val sql = """
            SELECT COUNT(*) AS cnt
            FROM tech_blog_subscription s
            INNER JOIN post p ON p.tech_blog_id = s.tech_blog_id
            WHERE s.member_id = :memberId
        """.trimIndent()

        return databaseClient.sql(sql)
            .bind("memberId", memberId)
            .map { row, _ -> row.get("cnt", Long::class.java) ?: 0L }
            .one()
            .awaitSingle()
    }
}
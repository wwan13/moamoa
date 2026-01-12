package server.feature.post.query

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
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
    private val bookmarkedPostReader: BookmarkedPostReader,
    private val postStatsReader: PostStatsReader,
) {

    suspend fun findAllByConditions(
        conditions: TechBlogPostQueryConditions,
        passport: Passport?,
    ): PostList = coroutineScope {

        val paging = Paging(
            size = conditions.size ?: 20,
            page = conditions.page ?: 1
        )

        val totalCountDeferred = async { countByTechBlogKey(conditions.techBlogKey) }
        val basePostsDeferred = async { loadPosts(paging, conditions.techBlogKey) }

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
            if (passport == null) emptySet()
            else bookmarkedPostReader.findBookmarkedPostIdSet(
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

    private suspend fun loadPosts(
        paging: Paging,
        techBlogKey: String,
    ): List<PostSummary> {
        if (paging.page > 5) {
            return fetchBasePostsByTechBlogKey(paging, techBlogKey).toList()
        }

        val cached = techBlogPostListCache.get(techBlogKey, paging.page)
        if (cached != null) return cached

        return fetchBasePostsByTechBlogKey(paging, techBlogKey).toList().also {
            techBlogPostListCache.set(techBlogKey, paging.page, it)
        }
    }

    private suspend fun fetchBasePostsByTechBlogKey(
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
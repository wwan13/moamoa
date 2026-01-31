package server.feature.post.query

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
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
    private val bookmarkedPostReader: BookmarkedPostReader,
    private val postStatsReader: PostStatsReader,
    private val cacheWarmupScope: CoroutineScope,
) {

    suspend fun findByConditions(
        conditions: PostQueryConditions,
        passport: Passport?
    ): PostList = coroutineScope {

        val paging = Paging(
            size = conditions.size ?: 20,
            page = conditions.page ?: 1
        )

        val totalCountDeferred = async { countAll(conditions.query) }
        val basePostsDeferred = async { loadPosts(paging, conditions.query) }

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
            postStatsReader.findPostStatsMap(postIds)
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
        query: String?
    ): List<PostSummary> {
        if (paging.page > 5 || !query.isNullOrBlank()) {
            return fetchBasePosts(paging, query).toList()
        }

        val cached = postListCache.get(paging.page, paging.size)
        if (cached != null) return cached

        return fetchBasePosts(paging).toList().also {
            cacheWarmupScope.launch {
                postListCache.set(paging.page, paging.size, it)
            }
        }
    }

    private suspend fun fetchBasePosts(
        paging: Paging,
        query: String? = null
    ): Flow<PostSummary> {
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

        var spec = databaseClient.sql(sql)
            .bind("limit", paging.size)
            .bind("offset", offset)

        if (query != null) {
            spec = spec.bind("keyword", "%$query%")
        }

        return spec
            .map { row, _ -> mapToPostSummary(row) }
            .all()
            .asFlow()
    }

    private suspend fun countAll(query: String?): Long {
        val whereClause = if (query != null) {
            "WHERE title LIKE :keyword OR description LIKE :keyword"
        } else ""

        val sql = """
        SELECT COUNT(*) AS cnt
        FROM post
        $whereClause
    """.trimIndent()

        var spec = databaseClient.sql(sql)
        if (query != null) spec = spec.bind("keyword", "%$query%")

        return spec
            .map { row, _ -> row.get("cnt", Long::class.java) ?: 0L }
            .one()
            .awaitSingle()
    }
}
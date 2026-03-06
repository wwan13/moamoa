package server.core.feature.post.query

import com.linecorp.kotlinjdsl.dsl.jpql.*
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Service
import server.core.feature.post.domain.Post
import server.core.feature.post.domain.PostTag
import server.core.feature.post.infra.PostListCache
import server.core.feature.tag.domain.Tag
import server.core.feature.techblog.domain.TechBlog
import server.core.global.security.Passport
import server.core.infra.cache.WarmupCoordinator
import server.core.support.paging.Paging
import server.core.support.paging.calculateTotalPage
import server.core.support.query.createJdslQuery

@Service
class PostQueryService(
    @PersistenceContext
    private val entityManager: EntityManager,
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

        val totalCount = countAllPosts(conditions.query)
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
        val limit = paging.size.toInt()
        val offset = (paging.page - 1L) * paging.size
        val jpqlQuery = createBasePostsQuery(query)

        val rows = entityManager
            .createJdslQuery(
                query = jpqlQuery,
                resultClass = PostSummary::class.java,
                offset = offset.toInt(),
                limit = limit,
            )
            .resultList

        return rows.distinctBy { it.id }
    }

    private fun countAllPosts(query: String?): Long {
        val jpqlQuery = createCountAllPostsQuery(query)

        return entityManager
            .createJdslQuery(
                query = jpqlQuery,
                resultClass = Long::class.javaObjectType,
                offset = 0,
                limit = Int.MAX_VALUE,
            )
            .resultList
            .firstOrNull()
            ?: 0L
    }

    private fun createBasePostsQuery(query: String?) = jpql {
        val keyword = query?.takeIf { it.isNotBlank() }?.let { "%$it%" }

        selectBasePostSummary(isBookmarked = false)
            .from(
                entity(Post::class),
                join(TechBlog::class).on(path(Post::techBlogId).equal(path(TechBlog::id))),
                leftJoin(PostTag::class).on(path(PostTag::postId).equal(path(Post::id))),
                leftJoin(Tag::class).on(path(PostTag::tagId).equal(path(Tag::id))),
            )
            .whereAnd(
                keyword?.let {
                    or(
                        path(Post::title).like(it),
                        path(Post::description).like(it),
                        path(Tag::title).like(it),
                    )
                }
            )
            .orderBy(path(Post::publishedAt).desc())
    }

    private fun createCountAllPostsQuery(query: String?) = jpql {
        val keyword = query?.takeIf { it.isNotBlank() }?.let { "%$it%" }
        select(count(path(Post::id)))
            .from(
                entity(Post::class)
            )
            .whereAnd(
                keyword?.let {
                    or(
                        path(Post::title).like(it),
                        path(Post::description).like(it),
                    )
                }
            )
    }
}

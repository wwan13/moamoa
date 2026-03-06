package server.core.feature.post.query

import com.linecorp.kotlinjdsl.dsl.jpql.*
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Service
import server.core.feature.post.domain.Post
import server.core.feature.post.infra.TechBlogPostListCache
import server.core.feature.techblog.domain.TechBlog
import server.core.global.security.Passport
import server.core.infra.cache.WarmupCoordinator
import server.core.support.paging.Paging
import server.core.support.paging.calculateTotalPage
import server.core.support.query.createJdslQuery

@Service
class TechBlogPostQueryService(
    @PersistenceContext
    private val entityManager: EntityManager,
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

        val totalCount = countTechBlogPosts(conditions.techBlogId)
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
        val bookmarkedIdSet =
            if (passport == null) emptySet()
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
        val limit = paging.size.toInt()
        val offset = (paging.page - 1L) * paging.size
        val jpqlQuery = createTechBlogBasePostsQuery(techBlogId)

        return entityManager
            .createJdslQuery(
                query = jpqlQuery,
                resultClass = PostSummary::class.java,
                offset = offset.toInt(),
                limit = limit,
            )
            .resultList
    }

    private fun countTechBlogPosts(techBlogId: Long): Long {
        val jpqlQuery = createCountTechBlogPostsQuery(techBlogId)

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

    private fun createTechBlogBasePostsQuery(techBlogId: Long) = jpql {
        selectBasePostSummary(isBookmarked = false)
            .from(
                entity(Post::class),
                join(TechBlog::class).on(path(Post::techBlogId).equal(path(TechBlog::id))),
            )
            .where(
                path(TechBlog::id).equal(techBlogId)
            )
            .orderBy(path(Post::publishedAt).desc())
    }

    private fun createCountTechBlogPostsQuery(techBlogId: Long) = jpql {
        select(count(path(Post::id)))
            .from(
                entity(Post::class)
            )
            .where(
                path(Post::techBlogId).equal(techBlogId)
            )
    }
}

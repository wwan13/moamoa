package server.core.feature.post.query

import com.linecorp.kotlinjdsl.dsl.jpql.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import server.core.feature.category.domain.Category
import server.core.feature.post.domain.Post
import server.core.feature.post.infra.TechBlogPostListCache
import server.core.feature.techblog.domain.TechBlog
import server.core.global.jdsl.JdslExecutor
import server.core.global.security.Passport
import server.core.infra.cache.WarmupCoordinator
import server.core.support.domain.ListEntry
import server.core.support.paging.Paging
import server.core.support.paging.calculateTotalPage

@Service
class TechBlogPostQueryService(
    private val jdslExecutor: JdslExecutor,
    private val techBlogPostListCache: TechBlogPostListCache,
    private val bookmarkedPostReader: BookmarkedPostReader,
    private val postStatsReader: PostStatsReader,
    private val warmupCoordinator: WarmupCoordinator,
) {

    @Transactional(readOnly = true)
    fun findAllByConditions(
        conditions: TechBlogPostQueryConditions,
        passport: Passport?,
    ): PostList {
        val paging = Paging(
            size = conditions.size ?: 20,
            page = conditions.page ?: 1
        )
        val categoryId = conditions.category?.let {
            Category.fromId(it)?.id ?: throw NoSuchElementException("존재하지 않는 카테고리입니다.")
        }

        val entry = loadEntry(paging, conditions.techBlogId, categoryId)
        val totalCount = entry.count
        val basePosts = entry.list

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

    private fun loadEntry(
        paging: Paging,
        techBlogId: Long,
        categoryId: Long?,
    ): ListEntry<PostSummary> {
        if (paging.page > 5) {
            return fetchEntry(paging, techBlogId, categoryId)
        }

        val cached = techBlogPostListCache.get(techBlogId, paging.page, categoryId)
        if (cached != null) return cached

        return fetchEntry(paging, techBlogId, categoryId).also { entry ->
            val warmupKey = techBlogPostListCache.key(techBlogId, paging.page, categoryId)
            warmupCoordinator.launchIfAbsent(warmupKey) {
                techBlogPostListCache.set(techBlogId, paging.page, categoryId, entry)
            }
        }
    }

    private fun fetchEntry(
        paging: Paging,
        techBlogId: Long,
        categoryId: Long?,
    ): ListEntry<PostSummary> {
        val count = fetchCountTechBlogPosts(techBlogId, categoryId)
        val list = fetchBasePostsByTechBlogKey(paging, techBlogId, categoryId)
        return ListEntry(
            count = count,
            list = list
        )
    }

    private fun fetchBasePostsByTechBlogKey(
        paging: Paging,
        techBlogId: Long,
        categoryId: Long?,
    ): List<PostSummary> {
        val limit = paging.size.toInt()
        val offset = (paging.page - 1L) * paging.size
        val jpqlQuery = createTechBlogBasePostsQuery(techBlogId, categoryId)

        return jdslExecutor
            .createQuery(
                query = jpqlQuery,
                resultClass = PostSummary::class.java,
                offset = offset.toInt(),
                limit = limit,
            )
            .resultList
    }

    private fun fetchCountTechBlogPosts(techBlogId: Long, categoryId: Long?): Long {
        val jpqlQuery = createCountTechBlogPostsQuery(techBlogId, categoryId)

        return jdslExecutor
            .createQuery(
                query = jpqlQuery,
                resultClass = Long::class.javaObjectType,
                offset = 0,
                limit = Int.MAX_VALUE,
            )
            .resultList
            .firstOrNull()
            ?: 0L
    }

    private fun createTechBlogBasePostsQuery(techBlogId: Long, categoryId: Long?) = jpql {
        selectBasePostSummary(isBookmarked = false)
            .from(
                entity(Post::class),
                join(TechBlog::class).on(path(Post::techBlogId).equal(path(TechBlog::id))),
            )
            .whereAnd(
                path(TechBlog::id).equal(techBlogId),
                categoryId?.let { path(Post::categoryId).equal(it) }
            )
            .orderBy(path(Post::publishedAt).desc())
    }

    private fun createCountTechBlogPostsQuery(techBlogId: Long, categoryId: Long?) = jpql {
        select(count(path(Post::id)))
            .from(
                entity(Post::class)
            )
            .whereAnd(
                path(Post::techBlogId).equal(techBlogId),
                categoryId?.let { path(Post::categoryId).equal(it) }
            )
    }

}

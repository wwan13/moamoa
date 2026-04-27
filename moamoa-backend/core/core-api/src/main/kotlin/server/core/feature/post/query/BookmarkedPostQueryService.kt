package server.core.feature.post.query

import com.linecorp.kotlinjdsl.dsl.jpql.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import server.core.feature.bookmark.domain.Bookmark
import server.core.feature.category.domain.Category
import server.core.feature.post.domain.Post
import server.core.feature.post.infra.BookmarkedPostListCache
import server.core.feature.techblog.domain.TechBlog
import server.core.global.jdsl.JdslExecutor
import server.core.global.security.Passport
import server.core.infra.cache.WarmupCoordinator
import server.core.support.domain.ListEntry
import server.core.support.paging.Paging
import server.core.support.paging.calculateTotalPage

@Service
class BookmarkedPostQueryService(
    private val jdslExecutor: JdslExecutor,
    private val bookmarkedPostListCache: BookmarkedPostListCache,
    private val postStatsReader: PostStatsReader,
    private val warmupCoordinator: WarmupCoordinator,
) {

    @Transactional(readOnly = true)
    fun findAllByConditions(
        conditions: PostQueryConditions,
        passport: Passport,
    ): PostList {
        val paging = Paging(
            size = conditions.size ?: 20,
            page = conditions.page ?: 1
        )
        val categoryId = conditions.category?.let {
            Category.fromId(it)?.id ?: throw NoSuchElementException("존재하지 않는 카테고리입니다.")
        }

        val entry = loadEntry(memberId = passport.memberId, paging = paging, categoryId = categoryId)
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

        val posts = basePosts.map { post ->
            post.copy(
                bookmarkCount = postStatsByPostId[post.id]?.bookmarkCount ?: post.bookmarkCount,
                viewCount = postStatsByPostId[post.id]?.viewCount ?: post.viewCount,
                isBookmarked = true,
            )
        }

        return PostList(meta, posts)
    }

    private fun loadEntry(memberId: Long, paging: Paging, categoryId: Long?): ListEntry<PostSummary> {
        if (paging.page > 5) {
            return fetchEntry(paging, memberId, categoryId)
        }

        val cached = bookmarkedPostListCache.get(memberId, paging.page, categoryId)
        if (cached != null) return cached

        return fetchEntry(paging, memberId, categoryId).also { entry ->
            val warmupKey = "${bookmarkedPostListCache.versionKey(memberId)}:CATEGORY:${categoryId ?: 0L}:PAGE:${paging.page}"
            warmupCoordinator.launchIfAbsent(warmupKey) {
                bookmarkedPostListCache.set(memberId, paging.page, categoryId, entry)
            }
        }
    }

    private fun fetchEntry(
        paging: Paging,
        memberId: Long,
        categoryId: Long?,
    ): ListEntry<PostSummary> {
        val count = fetchCountBookmarkedPosts(memberId, categoryId)
        val list = fetchBookmarkedBasePosts(paging, memberId, categoryId)
        return ListEntry(
            count = count,
            list = list
        )
    }

    private fun fetchBookmarkedBasePosts(
        paging: Paging,
        memberId: Long,
        categoryId: Long?,
    ): List<PostSummary> {
        val limit = paging.size.toInt()
        val offset = (paging.page - 1L) * paging.size
        val jpqlQuery = createBookmarkedBasePostsQuery(memberId, categoryId)

        return jdslExecutor
            .createQuery(
                query = jpqlQuery,
                resultClass = PostSummary::class.java,
                offset = offset.toInt(),
                limit = limit,
            )
            .resultList
    }

    private fun fetchCountBookmarkedPosts(memberId: Long, categoryId: Long?): Long {
        val jpqlQuery = createCountBookmarkedPostsQuery(memberId, categoryId)

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

    private fun createBookmarkedBasePostsQuery(memberId: Long, categoryId: Long?) = jpql {
        selectBasePostSummary(isBookmarked = true)
            .from(
                entity(Bookmark::class),
                join(Post::class).on(path(Bookmark::postId).equal(path(Post::id))),
                join(TechBlog::class).on(path(Post::techBlogId).equal(path(TechBlog::id))),
            )
            .whereAnd(
                path(Bookmark::memberId).equal(memberId),
                categoryId?.let { path(Post::categoryId).equal(it) }
            )
            .orderBy(path(Bookmark::createdAt).desc())
    }

    private fun createCountBookmarkedPostsQuery(memberId: Long, categoryId: Long?) = jpql {
        select(count(path(Bookmark::id)))
            .from(
                entity(Bookmark::class),
                join(Post::class).on(path(Bookmark::postId).equal(path(Post::id))),
            )
            .whereAnd(
                path(Bookmark::memberId).equal(memberId),
                categoryId?.let { path(Post::categoryId).equal(it) }
            )
    }

}

package server.core.feature.post.query

import com.linecorp.kotlinjdsl.dsl.jpql.*
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import server.core.feature.category.domain.Category
import server.core.feature.post.domain.Post
import server.core.feature.post.infra.SubscribedPostListCache
import server.core.feature.subscription.domain.Subscription
import server.core.feature.techblog.domain.TechBlog
import server.core.global.security.Passport
import server.core.infra.cache.WarmupCoordinator
import server.core.support.domain.ListEntry
import server.core.support.paging.Paging
import server.core.support.paging.calculateTotalPage
import server.core.support.query.createJdslQuery

@Service
class SubscribedPostQueryService(
    @PersistenceContext
    private val entityManager: EntityManager,
    private val subscribedPostListCache: SubscribedPostListCache,
    private val bookmarkedPostReader: BookmarkedPostReader,
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
        val bookmarkedIdSet = bookmarkedPostReader.findBookmarkedPostIdSet(
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

    private fun loadEntry(memberId: Long, paging: Paging, categoryId: Long?): ListEntry<PostSummary> {
        if (paging.page > 5) {
            return fetchEntry(paging, memberId, categoryId)
        }

        val cached = subscribedPostListCache.get(memberId, paging.page, categoryId)
        if (cached != null) return cached

        return fetchEntry(paging, memberId, categoryId).also { entry ->
            val warmupKey = "${subscribedPostListCache.versionKey(memberId)}:CATEGORY:${categoryId ?: 0L}:PAGE:${paging.page}"
            warmupCoordinator.launchIfAbsent(warmupKey) {
                subscribedPostListCache.set(memberId, paging.page, categoryId, entry)
            }
        }
    }

    private fun fetchEntry(
        paging: Paging,
        memberId: Long,
        categoryId: Long?,
    ): ListEntry<PostSummary> {
        val count = fetchCountSubscribedPosts(memberId, categoryId)
        val list = fetchSubscribingBasePosts(paging, memberId, categoryId)
        return ListEntry(
            count = count,
            list = list
        )
    }

    private fun fetchSubscribingBasePosts(
        paging: Paging,
        memberId: Long,
        categoryId: Long?,
    ): List<PostSummary> {
        val limit = paging.size.toInt()
        val offset = (paging.page - 1L) * paging.size
        val jpqlQuery = createSubscribedBasePostsQuery(memberId, categoryId)

        return entityManager
            .createJdslQuery(
                query = jpqlQuery,
                resultClass = PostSummary::class.java,
                offset = offset.toInt(),
                limit = limit,
            )
            .resultList
    }

    private fun fetchCountSubscribedPosts(memberId: Long, categoryId: Long?): Long {
        val jpqlQuery = createCountSubscribedPostsQuery(memberId, categoryId)

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

    private fun createSubscribedBasePostsQuery(memberId: Long, categoryId: Long?) = jpql {
        selectBasePostSummary(isBookmarked = false)
            .from(
                entity(Subscription::class),
                join(TechBlog::class).on(path(Subscription::techBlogId).equal(path(TechBlog::id))),
                join(Post::class).on(path(Post::techBlogId).equal(path(TechBlog::id))),
            )
            .whereAnd(
                path(Subscription::memberId).equal(memberId),
                categoryId?.let { path(Post::categoryId).equal(it) }
            )
            .orderBy(path(Post::publishedAt).desc())
    }

    private fun createCountSubscribedPostsQuery(memberId: Long, categoryId: Long?) = jpql {
        select(count(path(Post::id)))
            .from(
                entity(Subscription::class),
                join(Post::class).on(path(Post::techBlogId).equal(path(Subscription::techBlogId))),
            )
            .whereAnd(
                path(Subscription::memberId).equal(memberId),
                categoryId?.let { path(Post::categoryId).equal(it) }
            )
    }

}

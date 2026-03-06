package server.core.feature.post.query

import com.linecorp.kotlinjdsl.dsl.jpql.*
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Service
import server.core.feature.post.domain.Post
import server.core.feature.post.infra.SubscribedPostListCache
import server.core.feature.subscription.domain.Subscription
import server.core.feature.techblog.domain.TechBlog
import server.core.global.security.Passport
import server.core.infra.cache.WarmupCoordinator
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

    fun findAllByConditions(
        conditions: PostQueryConditions,
        passport: Passport,
    ): PostList {

        val paging = Paging(
            size = conditions.size ?: 20,
            page = conditions.page ?: 1
        )

        val totalCount = countSubscribedPosts(memberId = passport.memberId)
        val basePosts = loadPosts(memberId = passport.memberId, paging = paging)

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

    private fun loadPosts(memberId: Long, paging: Paging): List<PostSummary> {
        if (paging.page > 5) {
            return fetchSubscribingBasePosts(paging, memberId)
        }

        val cached = subscribedPostListCache.get(memberId, paging.page)
        if (cached != null) return cached

        return fetchSubscribingBasePosts(paging, memberId).also { posts ->
            val warmupKey = "${subscribedPostListCache.versionKey(memberId)}:PAGE:${paging.page}"
            warmupCoordinator.launchIfAbsent(warmupKey) {
                subscribedPostListCache.set(memberId, paging.page, posts)
            }
        }
    }

    private fun fetchSubscribingBasePosts(
        paging: Paging,
        memberId: Long,
    ): List<PostSummary> {
        val limit = paging.size.toInt()
        val offset = (paging.page - 1L) * paging.size
        val jpqlQuery = createSubscribedBasePostsQuery(memberId)

        return entityManager
            .createJdslQuery(
                query = jpqlQuery,
                resultClass = PostSummary::class.java,
                offset = offset.toInt(),
                limit = limit,
            )
            .resultList
    }

    private fun countSubscribedPosts(memberId: Long): Long {
        val jpqlQuery = createCountSubscribedPostsQuery(memberId)

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

    private fun createSubscribedBasePostsQuery(memberId: Long) = jpql {
        selectBasePostSummary(isBookmarked = false)
            .from(
                entity(Subscription::class),
                join(TechBlog::class).on(path(Subscription::techBlogId).equal(path(TechBlog::id))),
                join(Post::class).on(path(Post::techBlogId).equal(path(TechBlog::id))),
            )
            .where(
                path(Subscription::memberId).equal(memberId)
            )
            .orderBy(path(Post::publishedAt).desc())
    }

    private fun createCountSubscribedPostsQuery(memberId: Long) = jpql {
        select(count(path(Post::id)))
            .from(
                entity(Subscription::class),
                join(Post::class).on(path(Post::techBlogId).equal(path(Subscription::techBlogId))),
            )
            .where(
                path(Subscription::memberId).equal(memberId)
            )
    }
}

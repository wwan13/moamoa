package server.core.feature.post.query

import com.linecorp.kotlinjdsl.dsl.jpql.*
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import server.core.feature.category.domain.Category
import server.core.feature.post.domain.Post
import server.core.feature.post.domain.PostTag
import server.core.feature.post.infra.PostListCache
import server.core.feature.tag.domain.Tag
import server.core.feature.techblog.domain.TechBlog
import server.core.global.security.Passport
import server.core.infra.cache.WarmupCoordinator
import server.core.support.domain.ListEntry
import server.core.support.paging.Paging
import server.core.support.paging.calculateTotalPage
import server.core.support.query.createJdslQuery

@Service
@Transactional(readOnly = true)
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
        val categoryId = conditions.category?.let {
            Category.fromId(it)?.id ?: throw IllegalArgumentException("존재하지 않는 카테고리입니다.")
        }

        val entry = loadEntry(paging, conditions.query, categoryId)
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

    private fun loadEntry(
        paging: Paging,
        query: String?,
        categoryId: Long?,
    ): ListEntry<PostSummary> {
        if (paging.page > 5 || !query.isNullOrBlank()) {
            return fetchEntry(paging, query, categoryId)
        }

        val cached = postListCache.get(paging.page, paging.size, categoryId)
        if (cached != null) return cached

        return fetchEntry(paging, categoryId = categoryId).also { entry ->
            val warmupKey = postListCache.key(paging.page, paging.size, categoryId)
            warmupCoordinator.launchIfAbsent(warmupKey) {
                postListCache.set(paging.page, paging.size, categoryId, entry)
            }
        }
    }

    private fun fetchEntry(
        paging: Paging,
        query: String? = null,
        categoryId: Long? = null,
    ): ListEntry<PostSummary> {
        val count = fetchCountAllPosts(query, categoryId)
        val list = fetchBasePosts(paging, query, categoryId)
        return ListEntry(
            count = count,
            list = list
        )
    }

    private fun fetchBasePosts(
        paging: Paging,
        query: String? = null,
        categoryId: Long? = null,
    ): List<PostSummary> {
        val limit = paging.size.toInt()
        val offset = (paging.page - 1L) * paging.size
        val jpqlQuery = createBasePostsQuery(query, categoryId)

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

    private fun fetchCountAllPosts(query: String?, categoryId: Long?): Long {
        val jpqlQuery = createCountAllPostsQuery(query, categoryId)

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

    private fun createBasePostsQuery(query: String?, categoryId: Long?) = jpql {
        val keyword = query?.takeIf { it.isNotBlank() }?.let { "%$it%" }

        selectBasePostSummary(isBookmarked = false)
            .from(
                entity(Post::class),
                join(TechBlog::class).on(path(Post::techBlogId).equal(path(TechBlog::id))),
            )
            .whereAnd(
                categoryId?.let { path(Post::categoryId).equal(it) },
                keyword?.let {
                    or(
                        path(Post::title).like(it),
                        path(Post::description).like(it),
                        exists(
                            jpql {
                                select(path(PostTag::id))
                                    .from(
                                        entity(PostTag::class),
                                        join(Tag::class).on(path(PostTag::tagId).equal(path(Tag::id))),
                                    )
                                    .whereAnd(
                                        path(PostTag::postId).equal(path(Post::id)),
                                        path(Tag::title).like(it),
                                    )
                            }.asSubquery()
                        ),
                    )
                }
            )
            .orderBy(path(Post::publishedAt).desc())
    }

    private fun createCountAllPostsQuery(query: String?, categoryId: Long?) = jpql {
        val keyword = query?.takeIf { it.isNotBlank() }?.let { "%$it%" }
        select(countDistinct(Post::id))
            .from(
                entity(Post::class)
            )
            .whereAnd(
                categoryId?.let { path(Post::categoryId).equal(it) },
                keyword?.let {
                    or(
                        path(Post::title).like(it),
                        path(Post::description).like(it),
                        exists(
                            jpql {
                                select(path(PostTag::id))
                                    .from(
                                        entity(PostTag::class),
                                        join(Tag::class).on(path(PostTag::tagId).equal(path(Tag::id))),
                                    )
                                    .whereAnd(
                                        path(PostTag::postId).equal(path(Post::id)),
                                        path(Tag::title).like(it),
                                    )
                            }.asSubquery()
                        ),
                    )
                }
            )
    }

}

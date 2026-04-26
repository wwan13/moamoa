package server.admin.feature.post.query

import com.linecorp.kotlinjdsl.dsl.jpql.*
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import server.admin.feature.post.command.domain.AdminPost
import server.admin.feature.posttag.domain.AdminPostTag
import server.admin.feature.tag.domain.AdminTag
import server.admin.feature.techblog.application.AdminTechBlogData
import server.admin.feature.techblog.domain.AdminTechBlog
import server.admin.support.query.createJdslQuery

@Service
@Transactional(readOnly = true)
internal class AdminPostQueryService(
    @PersistenceContext
    private val entityManager: EntityManager,
) {
    fun findByConditions(conditions: AdminPostQueryConditions): AdminPostList {
        val size = conditions.size?.takeIf { it > 0 } ?: 20L
        val page = conditions.page?.takeIf { it > 0 } ?: 1L

        if (conditions.techBlogIds != null && conditions.techBlogIds.isEmpty()) {
            return AdminPostList(
                meta = AdminPostListMeta(page = page, size = size, totalCount = 0L, totalPages = 0L),
                posts = emptyList(),
            )
        }

        val totalCount = fetchTotalCount(conditions)
        val totalPages = if (totalCount == 0L) 0L else (totalCount + size - 1L) / size
        val offset = (page - 1L) * size

        val basePosts = fetchBasePosts(conditions, size, offset)
        val posts = if (basePosts.isEmpty()) emptyList() else {
            val tagsByPostId = fetchTagsByPostIds(basePosts.map { it.postId })
            basePosts.map { it.toSummary(tagsByPostId[it.postId].orEmpty()) }
        }

        return AdminPostList(
            meta = AdminPostListMeta(page = page, size = size, totalCount = totalCount, totalPages = totalPages),
            posts = posts,
        )
    }

    private fun fetchBasePosts(
        conditions: AdminPostQueryConditions,
        size: Long,
        offset: Long,
    ): List<AdminPostRow> {
        val jpqlQuery = createBasePostsQuery(conditions)

        return entityManager
            .createJdslQuery(
                query = jpqlQuery,
                resultClass = AdminPostRow::class.java,
                offset = offset.toInt(),
                limit = size.toInt(),
            )
            .resultList
    }

    private fun fetchTotalCount(conditions: AdminPostQueryConditions): Long {
        val jpqlQuery = createCountPostsQuery(conditions)

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

    private fun fetchTagsByPostIds(postIds: List<Long>): Map<Long, List<AdminTag>> {
        if (postIds.isEmpty()) return emptyMap()

        val jpqlQuery = jpql {
            selectNew<AdminPostTagRow>(
                path(AdminPostTag::postId),
                path(AdminTag::id),
                path(AdminTag::title),
            )
                .from(
                    entity(AdminPostTag::class),
                    join(AdminTag::class).on(path(AdminTag::id).equal(path(AdminPostTag::tagId))),
                )
                .where(path(AdminPostTag::postId).`in`(postIds))
                .orderBy(
                    path(AdminPostTag::postId).asc(),
                    path(AdminTag::title).asc(),
                )
        }

        val rows = entityManager
            .createJdslQuery(
                query = jpqlQuery,
                resultClass = AdminPostTagRow::class.java,
                offset = 0,
                limit = Int.MAX_VALUE,
            )
            .resultList

        return rows.groupBy(
            keySelector = { it.postId },
            valueTransform = { AdminTag(id = it.tagId, title = it.tagTitle) },
        )
    }

    private fun createBasePostsQuery(conditions: AdminPostQueryConditions) = jpql {
        val keyword = conditions.query?.takeIf { it.isNotBlank() }?.let { "%$it%" }

        selectBaseAdminPostRow()
            .from(
                entity(AdminPost::class),
                join(AdminTechBlog::class).on(path(AdminTechBlog::id).equal(path(AdminPost::techBlogId))),
            )
            .whereAnd(
                keyword?.let {
                    or(
                        path(AdminPost::title).like(it),
                        path(AdminPost::description).like(it),
                        exists(
                            jpql {
                                select(path(AdminPostTag::id))
                                    .from(
                                        entity(AdminPostTag::class),
                                        join(AdminTag::class).on(path(AdminTag::id).equal(path(AdminPostTag::tagId))),
                                    )
                                    .whereAnd(
                                        path(AdminPostTag::postId).equal(path(AdminPost::id)),
                                        path(AdminTag::title).like(it),
                                    )
                            }.asSubquery()
                        ),
                    )
                },
                conditions.categoryId?.let { path(AdminPost::categoryId).equal(it) },
                conditions.techBlogIds?.takeIf { it.isNotEmpty() }?.let { path(AdminPost::techBlogId).`in`(it) },
            )
            .orderBy(path(AdminPost::publishedAt).desc())
    }

    private fun createCountPostsQuery(conditions: AdminPostQueryConditions) = jpql {
        val keyword = conditions.query?.takeIf { it.isNotBlank() }?.let { "%$it%" }

        select(countDistinct(AdminPost::id))
            .from(entity(AdminPost::class))
            .whereAnd(
                keyword?.let {
                    or(
                        path(AdminPost::title).like(it),
                        path(AdminPost::description).like(it),
                        exists(
                            jpql {
                                select(path(AdminPostTag::id))
                                    .from(
                                        entity(AdminPostTag::class),
                                        join(AdminTag::class).on(path(AdminTag::id).equal(path(AdminPostTag::tagId))),
                                    )
                                    .whereAnd(
                                        path(AdminPostTag::postId).equal(path(AdminPost::id)),
                                        path(AdminTag::title).like(it),
                                    )
                            }.asSubquery()
                        ),
                    )
                },
                conditions.categoryId?.let { path(AdminPost::categoryId).equal(it) },
                conditions.techBlogIds?.takeIf { it.isNotEmpty() }?.let { path(AdminPost::techBlogId).`in`(it) },
            )
    }

    private fun AdminPostRow.toSummary(tags: List<AdminTag>) = AdminPostSummary(
        postId = postId,
        key = key,
        title = title,
        description = description,
        thumbnail = thumbnail,
        url = url,
        publishedAt = publishedAt,
        categoryId = categoryId,
        techBlog = AdminTechBlogData(
            id = techBlogId,
            title = techBlogTitle,
            icon = techBlogIcon,
            blogUrl = techBlogBlogUrl,
            key = techBlogKey,
        ),
        tags = tags,
    )
}

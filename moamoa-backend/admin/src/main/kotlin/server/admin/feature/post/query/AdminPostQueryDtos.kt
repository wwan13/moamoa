package server.admin.feature.post.query

import server.admin.feature.tag.domain.AdminTag
import server.admin.feature.techblog.application.AdminTechBlogData
import java.time.LocalDateTime

internal data class AdminPostQueryConditions(
    val page: Long?,
    val size: Long?,
    val query: String?,
    val categoryId: Long?,
    val techBlogIds: Set<Long>?,
)

internal data class AdminPostList(
    val meta: AdminPostListMeta,
    val posts: List<AdminPostSummary>
)

internal data class AdminPostSummary(
    val postId: Long,
    val key: String,
    val title: String,
    val description: String,
    val thumbnail: String,
    val url: String,
    val publishedAt: LocalDateTime,
    val categoryId: Long,
    val techBlog: AdminTechBlogData,
    val tags: List<AdminTag>
)

internal data class AdminPostListMeta(
    val page: Long,
    val size: Long,
    val totalCount: Long,
    val totalPages: Long
)

internal data class AdminPostRow(
    val postId: Long,
    val key: String,
    val title: String,
    val description: String,
    val thumbnail: String,
    val url: String,
    val publishedAt: LocalDateTime,
    val categoryId: Long,
    val techBlogId: Long,
    val techBlogTitle: String,
    val techBlogIcon: String,
    val techBlogBlogUrl: String,
    val techBlogKey: String,
)

internal data class AdminPostTagRow(
    val postId: Long,
    val tagId: Long,
    val tagTitle: String,
)

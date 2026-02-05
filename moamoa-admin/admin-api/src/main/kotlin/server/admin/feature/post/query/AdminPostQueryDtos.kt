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

internal data class AdminPostSummary(
    val postId: Long,
    val key: String,
    val title: String,
    val description: String,
    val thumbnail: String,
    val url: String,
    val publishedAt: LocalDateTime,
    val techBlog: AdminTechBlogData,
    val tags: List<AdminTag>
)
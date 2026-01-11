package server.feature.post.query

import server.feature.techblog.command.application.TechBlogData
import java.time.LocalDateTime

data class PostQueryConditions(
    val page: Long?,
    val size: Long?,
)

data class PostList(
    val meta: PostListMeta,
    val posts: List<PostSummary>
)

data class PostListMeta(
    val page: Long,
    val size: Long,
    val totalCount: Long,
    val totalPages: Long
)

data class PostSummary(
    val id: Long,
    val key: String,
    val title: String,
    val description: String,
    val thumbnail: String,
    val url: String,
    val publishedAt: LocalDateTime,
    val isBookmarked: Boolean,
    val viewCount: Long,
    val bookmarkCount: Long,
    val techBlog: TechBlogData
)

data class TechBlogPostQueryConditions(
    val techBlogKey: String,
    val page: Long?,
    val size: Long?
)

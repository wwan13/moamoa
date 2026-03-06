package server.core.feature.post.query

import java.time.LocalDateTime

data class PostQueryConditions(
    val page: Long?,
    val size: Long?,
    val query: String?,
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
    val techBlogId: Long,
    val techBlogTitle: String,
    val techBlogIcon: String,
    val techBlogBlogUrl: String,
    val techBlogKey: String,
    val techBlogSubscriptionCount: Long,
)

data class TechBlogPostQueryConditions(
    val techBlogId: Long,
    val page: Long?,
    val size: Long?
)

data class PostStats(
    val postId: Long,
    val viewCount: Long,
    val bookmarkCount: Long,
)

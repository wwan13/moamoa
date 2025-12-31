package server.application

import server.domain.post.Post
import server.domain.techblog.TechBlog
import java.time.LocalDateTime

data class IncreaseViewCountResult(
    val success: Boolean
)

data class PostData(
    val id: Long,
    val key: String,
    val title: String,
    val description: String,
    val thumbnail: String,
    val url: String,
    val publishedAt: LocalDateTime,
    val viewCount: Long,
    val bookmarkCount: Long,
    val techBlogId: Long
) {
  constructor(post: Post) : this(
      id = post.id,
      key = post.key,
      title = post.title,
      description = post.description,
      thumbnail = post.thumbnail,
      url = post.url,
      publishedAt = post.publishedAt,
      viewCount = post.viewCount,
      bookmarkCount = post.bookmarkCount,
      techBlogId = post.techBlogId
  )
}

data class PostQueryConditions(
    val page: Long?,
    val size: Long?,
    val techBlogId: Long?
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

data class PostList(
    val meta: ListMeta,
    val posts: List<PostSummary>
)

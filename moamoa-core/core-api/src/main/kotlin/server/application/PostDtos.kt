package server.application

import server.domain.post.Post
import server.domain.techblog.TechBlog

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
      viewCount = post.viewCount,
      bookmarkCount = post.bookmarkCount,
      techBlogId = post.techBlogId
  )
}

data class PostQueryConditions(
    val page: Long?,
    val size: Long?
)

data class PostSummary(
    val id: Long,
    val key: String,
    val title: String,
    val description: String,
    val thumbnail: String,
    val url: String,
    val viewCount: Long,
    val bookmarkCount: Long,
    val techBlog: TechBlogData
) {
    constructor(post: Post, techBlog: TechBlog) : this(
        id = post.id,
        key = post.key,
        title = post.title,
        description = post.description,
        thumbnail = post.thumbnail,
        url = post.url,
        viewCount = post.viewCount,
        bookmarkCount = post.bookmarkCount,
        techBlog = TechBlogData(techBlog)
    )
}

data class PostList(
    val meta: ListMeta,
    val posts: List<PostSummary>
)

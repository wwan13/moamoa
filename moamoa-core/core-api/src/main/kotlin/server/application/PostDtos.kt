package server.application

import server.domain.post.Post

data class PostData(
    val id: Long,
    val key: String,
    val title: String,
    val description: String,
    val thumbnail: String,
    val url: String,
    val viewCount: Long,
    val techBlogId: Long
) {
  constructor(post: Post) : this(
      post.id,
      post.key,
      post.title,
      post.description,
      post.thumbnail,
      post.url,
      post.viewCount,
      post.techBlogId
  )
}
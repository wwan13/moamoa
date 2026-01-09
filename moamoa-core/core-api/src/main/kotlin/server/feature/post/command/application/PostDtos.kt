package server.feature.post.command.application

import server.feature.post.command.domain.Post
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

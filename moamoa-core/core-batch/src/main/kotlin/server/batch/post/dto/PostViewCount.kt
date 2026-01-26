package server.batch.post.dto

internal data class PostViewCount(
    val postId: Long,
    val viewCount: Long,
)
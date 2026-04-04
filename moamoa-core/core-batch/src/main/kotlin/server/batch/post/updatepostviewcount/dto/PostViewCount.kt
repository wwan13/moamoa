package server.batch.post.updatepostviewcount.dto

internal data class PostViewCount(
    val postId: Long,
    val delta: Long,
    val cacheKey: String,
)

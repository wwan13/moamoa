package server.batch.post.dto

internal data class PostSummary(
    val postId: Long,
    val title: String,
    val description: String,
    val key: String,
    val tags: List<String>
)

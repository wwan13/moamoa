package server.batch.post.dto

internal data class PreCategorizingPostResult(
    val categorized: List<PostCategory>,
    val uncategorized: List<PostSummary>
)

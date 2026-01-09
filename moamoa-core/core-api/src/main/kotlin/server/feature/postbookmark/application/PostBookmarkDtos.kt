package server.feature.postbookmark.application

data class PostBookmarkToggleCommand(
    val postId: Long
)

data class PostBookmarkToggleResult(
    val bookmarked: Boolean
)

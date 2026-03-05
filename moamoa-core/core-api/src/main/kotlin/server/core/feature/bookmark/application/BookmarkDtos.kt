package server.core.feature.bookmark.application

data class BookmarkToggleCommand(
    val postId: Long
)

data class BookmarkToggleResult(
    val bookmarked: Boolean
)

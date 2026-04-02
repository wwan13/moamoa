package server.core.feature.bookmark.application

data class BookmarkCommand(
    val postId: Long
)

data class BookmarkResult(
    val bookmarked: Boolean
)

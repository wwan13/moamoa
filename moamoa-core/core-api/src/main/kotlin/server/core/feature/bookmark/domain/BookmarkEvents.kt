package server.core.feature.bookmark.domain

data class BookmarkUpdatedEvent(
    val memberId: Long,
    val postId: Long,
    val bookmarked: Boolean
)

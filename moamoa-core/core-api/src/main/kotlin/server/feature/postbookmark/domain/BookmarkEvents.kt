package server.feature.postbookmark.domain

data class PostBookmarkUpdatedEvent(
    val memberId: Long,
    val postId: Long,
    val bookmarked: Boolean
)

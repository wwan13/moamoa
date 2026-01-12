package server.feature.postbookmark.domain

data class PostBookmarkCreatedEvent(
    val memberId: Long,
    val postId: Long
)

data class PostBookmarkRemovedEvent(
    val memberId: Long,
    val postId: Long,
)

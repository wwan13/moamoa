package server.domain.postbookmark

data class PostBookmarkCreatedEvent(
    val postId: Long
)

data class PostBookmarkRemovedEvent(
    val postId: Long,
)

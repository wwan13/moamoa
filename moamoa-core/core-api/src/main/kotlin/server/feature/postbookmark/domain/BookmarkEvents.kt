package server.feature.postbookmark.domain

data class PostBookmarkCreatedEvent(
    val postId: Long
)

data class PostBookmarkRemovedEvent(
    val postId: Long,
)

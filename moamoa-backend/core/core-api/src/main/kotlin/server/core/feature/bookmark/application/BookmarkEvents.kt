package server.core.feature.bookmark.application

import server.messaging.Event

data class BookmarkUpdatedEvent(
    val memberId: Long,
    val postId: Long,
    val bookmarked: Boolean
) : Event

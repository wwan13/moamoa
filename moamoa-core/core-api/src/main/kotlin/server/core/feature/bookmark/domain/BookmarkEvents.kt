package server.core.feature.bookmark.domain

import server.messaging.Event

data class BookmarkUpdatedEvent(
    val memberId: Long,
    val postId: Long,
    val bookmarked: Boolean
) : Event

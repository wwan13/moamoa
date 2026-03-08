package server.core.feature.bookmark.domain

import server.core.support.domain.DomainEvent

data class BookmarkUpdatedEvent(
    val memberId: Long,
    val postId: Long,
    val bookmarked: Boolean
) : DomainEvent

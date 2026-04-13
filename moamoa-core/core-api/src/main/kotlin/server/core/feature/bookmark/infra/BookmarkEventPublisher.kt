package server.core.feature.bookmark.infra

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import server.core.feature.bookmark.domain.Bookmark
import server.core.feature.bookmark.domain.BookmarkUpdatedEvent
import server.core.infra.outbox.TransactionalEventPublisher

@Component
class BookmarkEventPublisher(
    private val eventPublisher: TransactionalEventPublisher
) {

    @Transactional(propagation = Propagation.MANDATORY)
    fun publishBookmarked(bookmark: Bookmark) {
        eventPublisher.publish(
            BookmarkUpdatedEvent(
                memberId = bookmark.memberId,
                postId = bookmark.postId,
                bookmarked = true,
            )
        )
    }

    @Transactional(propagation = Propagation.MANDATORY)
    fun publishUnbookmarked(bookmark: Bookmark) {
        eventPublisher.publish(
            BookmarkUpdatedEvent(
                memberId = bookmark.memberId,
                postId = bookmark.postId,
                bookmarked = false,
            )
        )
    }
}
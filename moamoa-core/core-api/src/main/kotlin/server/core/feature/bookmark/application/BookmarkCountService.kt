package server.core.feature.bookmark.application

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import server.core.feature.bookmark.domain.BookmarkUpdatedEvent
import server.core.feature.post.domain.PostRepository
import server.core.infra.db.transaction.HandleTransactionEvent
import server.messaging.EventHandler
import server.messaging.SubscriptionDefinition

@Service
class BookmarkCountService(
    private val countProcessingStream: SubscriptionDefinition,
    private val postRepository: PostRepository,
    private val handleTransactionEvent: HandleTransactionEvent,
) {
    @EventHandler
    fun bookmarkUpdatedCountCalculate() =
        handleTransactionEvent(countProcessingStream, BookmarkUpdatedEvent::class.java) { event ->
            val delta = if (event.bookmarked) 1L else -1L
            postRepository.findByIdOrNull(event.postId)?.updateBookmarkCount(delta)
        }
}

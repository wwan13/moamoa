package server.core.feature.bookmark.application

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import server.core.feature.bookmark.domain.BookmarkUpdatedEvent
import server.core.feature.post.domain.PostRepository
import server.messaging.definition.EventStream
import server.messaging.annotation.TransactionEventHandler

@Service
class BookmarkCountService(
    private val postRepository: PostRepository,
) {
    @TransactionEventHandler(BookmarkUpdatedEvent::class, EventStream.COUNT_PROCESSING)
    fun bookmarkUpdatedCountCalculate(event: BookmarkUpdatedEvent) {
        val delta = if (event.bookmarked) 1L else -1L
        postRepository.findByIdOrNull(event.postId)?.updateBookmarkCount(delta)
    }
}

package server.core.feature.bookmark.application

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import server.core.feature.bookmark.domain.BookmarkUpdatedEvent
import server.core.feature.post.domain.PostRepository
import server.core.infra.messagebroker.TransactionalEventHandler
import server.messaging.SubscriptionDefinition
import server.messaging.invoke

@Service
class BookmarkCountService(
    private val countProcessingStream: SubscriptionDefinition,
    private val postRepository: PostRepository,
    private val transactionalEventHandler: TransactionalEventHandler,
) {
    fun bookmarkUpdatedCountCalculate() =
        transactionalEventHandler<BookmarkUpdatedEvent>(countProcessingStream) { event ->
            val delta = if (event.bookmarked) 1L else -1L
            postRepository.findByIdOrNull(event.postId)?.updateBookmarkCount(delta)
        }
}

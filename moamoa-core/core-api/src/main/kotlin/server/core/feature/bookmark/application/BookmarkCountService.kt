package server.core.feature.bookmark.application

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import server.core.feature.bookmark.domain.BookmarkUpdatedEvent
import server.core.feature.post.domain.PostRepository
import server.core.infra.db.transaction.Transactional
import server.messaging.EventHandler
import server.messaging.SubscriptionDefinition
import server.messaging.handleMessage

@Service
class BookmarkCountService(
    private val countProcessingStream: SubscriptionDefinition,
    private val postRepository: PostRepository,
    private val transactional: Transactional,
) {
    @EventHandler
    fun bookmarkUpdatedCountCalculate() =
        handleMessage<BookmarkUpdatedEvent>(countProcessingStream) { event ->
            val delta = if (event.bookmarked) 1L else -1L
            transactional {
                val post = postRepository.findByIdOrNull(event.postId) ?: return@transactional
                post.updateBookmarkCount(delta)
            }
        }
}

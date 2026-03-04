package server.feature.postbookmark.application

import org.springframework.stereotype.Service
import server.feature.postbookmark.domain.PostBookmarkUpdatedEvent
import server.infra.db.transaction.Transactional
import server.shared.messaging.EventHandler
import server.shared.messaging.SubscriptionDefinition
import server.shared.messaging.handleMessage
import org.springframework.data.repository.findByIdOrNull
import server.feature.post.command.domain.PostRepository

@Service
class PostBookmarkCountService(
    private val countProcessingStream: SubscriptionDefinition,
    private val postRepository: PostRepository,
    private val transactional: Transactional,
) {
    @EventHandler
    fun bookmarkUpdatedCountCalculate() =
        handleMessage<PostBookmarkUpdatedEvent>(countProcessingStream) { event ->
            val delta = if (event.bookmarked) 1L else -1L
            transactional {
                val post = postRepository.findByIdOrNull(event.postId) ?: return@transactional
                post.updateBookmarkCount(delta)
            }
        }
}

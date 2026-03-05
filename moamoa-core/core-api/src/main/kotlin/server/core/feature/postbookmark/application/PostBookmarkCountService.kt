package server.core.feature.postbookmark.application

import org.springframework.stereotype.Service
import server.core.feature.postbookmark.domain.PostBookmarkUpdatedEvent
import server.core.infra.db.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import server.core.feature.post.domain.PostRepository

@Service
class PostBookmarkCountService(
    private val countProcessingStream: server.messaging.SubscriptionDefinition,
    private val postRepository: PostRepository,
    private val transactional: Transactional,
) {
    @server.messaging.EventHandler
    fun bookmarkUpdatedCountCalculate() =
        _root_ide_package_.server.messaging.handleMessage<PostBookmarkUpdatedEvent>(countProcessingStream) { event ->
            val delta = if (event.bookmarked) 1L else -1L
            transactional {
                val post = postRepository.findByIdOrNull(event.postId) ?: return@transactional
                post.updateBookmarkCount(delta)
            }
        }
}

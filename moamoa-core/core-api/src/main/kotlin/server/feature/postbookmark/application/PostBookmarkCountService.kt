package server.feature.postbookmark.application

import org.springframework.stereotype.Service
import server.feature.post.command.domain.PostRepository
import server.feature.postbookmark.domain.PostBookmarkUpdatedEvent
import server.shared.messaging.EventHandler
import server.shared.messaging.SubscriptionDefinition
import server.shared.messaging.handleMessage

@Service
class PostBookmarkCountService(
    private val countProcessingStream: SubscriptionDefinition,
    private val postRepository: PostRepository
) {
    @EventHandler
    fun bookmarkUpdatedCountCalculate() =
        handleMessage<PostBookmarkUpdatedEvent>(countProcessingStream) { event ->
            val delta = if (event.bookmarked) 1L else -1L
            postRepository.incrementBookmarkCount(event.postId, delta)
        }
}
package server.feature.postbookmark.application

import org.springframework.stereotype.Service
import server.feature.post.command.domain.PostRepository
import server.feature.postbookmark.domain.PostBookmarkUpdatedEvent
import server.messaging.EventHandler
import server.messaging.StreamDefinition
import server.messaging.handleEvent

@Service
class PostBookmarkCountService(
    private val countProcessingStream: StreamDefinition,
    private val postRepository: PostRepository
) {
    @EventHandler
    fun bookmarkUpdatedCountCalculate() =
        handleEvent<PostBookmarkUpdatedEvent>(countProcessingStream) { event ->
            val delta = if (event.bookmarked) 1L else -1L
            postRepository.incrementBookmarkCount(event.postId, delta)
        }
}
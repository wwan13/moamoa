package server.application

import org.springframework.stereotype.Service
import server.domain.post.PostRepository
import server.domain.postbookmark.PostBookmarkCreatedEvent
import server.domain.postbookmark.PostBookmarkRemovedEvent
import server.messaging.EventHandler
import server.messaging.StreamDefinition
import server.messaging.handleEvent

@Service
class PostBookmarkCountService(
    private val defaultStream: StreamDefinition,
    private val postRepository: PostRepository
) {
    @EventHandler
    fun bookmarkCreated() =
        handleEvent<PostBookmarkCreatedEvent>(defaultStream) { event ->
            postRepository.incrementBookmarkCount(event.postId, +1)
        }

    @EventHandler
    fun bookmarkRemoved() =
        handleEvent<PostBookmarkRemovedEvent>(defaultStream) { event ->
            postRepository.incrementBookmarkCount(event.postId, -1)
        }
}
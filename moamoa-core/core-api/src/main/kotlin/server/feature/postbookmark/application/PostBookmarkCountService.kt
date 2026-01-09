package server.feature.postbookmark.application

import org.springframework.stereotype.Service
import server.feature.post.command.domain.PostRepository
import server.feature.postbookmark.domain.PostBookmarkCreatedEvent
import server.feature.postbookmark.domain.PostBookmarkRemovedEvent
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
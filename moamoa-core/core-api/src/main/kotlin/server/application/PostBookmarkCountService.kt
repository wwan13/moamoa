package server.application

import kotlinx.coroutines.reactor.mono
import org.springframework.stereotype.Service
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import server.domain.post.PostRepository
import server.domain.postbookmark.PostBookmarkCreatedEvent
import server.domain.postbookmark.PostBookmarkRemovedEvent

@Service
class PostBookmarkCountService(
    private val postRepository: PostRepository
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun bookmarkCreated(event: PostBookmarkCreatedEvent) = mono {
        postRepository.incrementBookmarkCount(event.postId, +1)
    }.subscribe()

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun bookmarkRemoved(event: PostBookmarkRemovedEvent) = mono {
        postRepository.incrementBookmarkCount(event.postId, -1)
    }.subscribe()
}
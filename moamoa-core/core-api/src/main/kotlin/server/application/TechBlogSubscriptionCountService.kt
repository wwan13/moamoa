package server.application

import kotlinx.coroutines.reactor.mono
import org.springframework.stereotype.Service
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import server.domain.postbookmark.PostBookmarkCreatedEvent
import server.domain.postbookmark.PostBookmarkRemovedEvent
import server.domain.techblog.TechBlogRepository
import server.domain.techblog.TechBlogSubscribeCreatedEvent
import server.domain.techblog.TechBlogSubscribeRemovedEvent

@Service
class TechBlogSubscriptionCountService(
    private val techBlogRepository: TechBlogRepository
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun subscriptionCreated(event: TechBlogSubscribeCreatedEvent) = mono {
        techBlogRepository.incrementSubscriptionCount(event.techBlogId, +1)
    }.subscribe()

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun subscriptionRemoved(event: TechBlogSubscribeRemovedEvent) = mono {
        techBlogRepository.incrementSubscriptionCount(event.techBlogId, -1)
    }.subscribe()
}
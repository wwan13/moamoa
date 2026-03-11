package server.core.feature.subscription.application

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import server.core.feature.subscription.domain.TechBlogSubscribeUpdatedEvent
import server.core.feature.techblog.domain.TechBlogRepository
import server.messaging.annotation.EventStream
import server.messaging.annotation.TransactionEventHandler

@Service
class SubscriptionCountService(
    private val techBlogRepository: TechBlogRepository,
) {
    @TransactionEventHandler(TechBlogSubscribeUpdatedEvent::class, EventStream.COUNT_PROCESSING)
    fun subscriptionUpdatedCountCalculate(event: TechBlogSubscribeUpdatedEvent) {
        val delta = if (event.subscribed) +1L else -1L
        techBlogRepository.findByIdOrNull(event.techBlogId)?.updateSubscriptionCount(delta)
    }
}

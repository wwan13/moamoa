package server.core.feature.subscription.application

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import server.core.feature.subscription.domain.TechBlogSubscribeUpdatedEvent
import server.core.feature.techblog.domain.TechBlogRepository
import server.core.infra.db.transaction.HandleTransactionEvent
import server.messaging.EventHandler

@Service
class SubscriptionCountService(
    private val countProcessingStream: server.messaging.SubscriptionDefinition,
    private val techBlogRepository: TechBlogRepository,
    private val handleTransactionEvent: HandleTransactionEvent,
) {
    @EventHandler
    fun subscriptionUpdatedCountCalculate() =
        handleTransactionEvent(countProcessingStream, TechBlogSubscribeUpdatedEvent::class.java) { event ->
            val delta = if (event.subscribed) +1L else -1L
            techBlogRepository.findByIdOrNull(event.techBlogId)?.updateSubscriptionCount(delta)
        }
}

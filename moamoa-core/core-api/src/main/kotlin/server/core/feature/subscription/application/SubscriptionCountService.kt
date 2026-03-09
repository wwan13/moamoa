package server.core.feature.subscription.application

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import server.core.feature.subscription.domain.TechBlogSubscribeUpdatedEvent
import server.core.feature.techblog.domain.TechBlogRepository
import server.core.infra.messagebroker.TransactionalEventHandler
import server.messaging.invoke

@Service
class SubscriptionCountService(
    private val countProcessingStream: server.messaging.SubscriptionDefinition,
    private val techBlogRepository: TechBlogRepository,
    private val transactionalEventHandler: TransactionalEventHandler,
) {
    fun subscriptionUpdatedCountCalculate() =
        transactionalEventHandler<TechBlogSubscribeUpdatedEvent>(countProcessingStream) { event ->
            val delta = if (event.subscribed) +1L else -1L
            techBlogRepository.findByIdOrNull(event.techBlogId)?.updateSubscriptionCount(delta)
        }
}

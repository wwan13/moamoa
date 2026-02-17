package server.feature.techblogsubscription.application

import org.springframework.stereotype.Service
import server.feature.techblog.command.domain.TechBlogRepository
import server.feature.techblogsubscription.domain.TechBlogSubscribeUpdatedEvent
import server.shared.messaging.EventHandler
import server.shared.messaging.SubscriptionDefinition
import server.shared.messaging.handleMessage

@Service
class TechBlogSubscriptionCountService(
    private val countProcessingStream: SubscriptionDefinition,
    private val techBlogRepository: TechBlogRepository
) {
    @EventHandler
    fun subscriptionUpdatedCountCalculate() =
        handleMessage<TechBlogSubscribeUpdatedEvent>(countProcessingStream) { event ->
            val delta = if (event.subscribed) +1L else -1L
            techBlogRepository.incrementSubscriptionCount(event.techBlogId, delta)
        }
}
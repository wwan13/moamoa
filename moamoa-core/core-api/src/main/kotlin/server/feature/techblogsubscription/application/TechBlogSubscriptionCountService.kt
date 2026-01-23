package server.feature.techblogsubscription.application

import org.springframework.stereotype.Service
import server.feature.techblog.command.domain.TechBlogRepository
import server.feature.techblogsubscription.domain.TechBlogSubscribeUpdatedEvent
import server.messaging.EventHandler
import server.messaging.StreamDefinition
import server.messaging.handleEvent

@Service
class TechBlogSubscriptionCountService(
    private val countProcessingStream: StreamDefinition,
    private val techBlogRepository: TechBlogRepository
) {
    @EventHandler
    fun subscriptionUpdatedCountCalculate() =
        handleEvent<TechBlogSubscribeUpdatedEvent>(countProcessingStream) { event ->
            val delta = if (event.subscribed) +1L else -1L
            techBlogRepository.incrementSubscriptionCount(event.techBlogId, delta)
        }
}
package server.feature.techblogsubscription.application

import org.springframework.stereotype.Service
import server.feature.techblog.command.domain.TechBlogRepository
import server.feature.techblog.command.domain.TechBlogSubscribeCreatedEvent
import server.feature.techblog.command.domain.TechBlogSubscribeRemovedEvent
import server.messaging.EventHandler
import server.messaging.StreamDefinition
import server.messaging.handleEvent

@Service
class TechBlogSubscriptionCountService(
    private val countProcessingStream: StreamDefinition,
    private val techBlogRepository: TechBlogRepository
) {
    @EventHandler
    fun subscriptionCreated() =
        handleEvent<TechBlogSubscribeCreatedEvent>(countProcessingStream) { event ->
            techBlogRepository.incrementSubscriptionCount(event.techBlogId, +1)
        }

    @EventHandler
    fun subscriptionRemoved() =
        handleEvent<TechBlogSubscribeRemovedEvent>(countProcessingStream) { event ->
            techBlogRepository.incrementSubscriptionCount(event.techBlogId, -1)
        }
}
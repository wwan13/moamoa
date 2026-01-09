package server.feature.techblogsubscription.application

import org.springframework.stereotype.Service
import server.feature.techblog.domain.TechBlogRepository
import server.feature.techblog.domain.TechBlogSubscribeCreatedEvent
import server.feature.techblog.domain.TechBlogSubscribeRemovedEvent
import server.messaging.EventHandler
import server.messaging.StreamDefinition
import server.messaging.handleEvent

@Service
class TechBlogSubscriptionCountService(
    private val defaultStream: StreamDefinition,
    private val techBlogRepository: TechBlogRepository
) {
    @EventHandler
    fun subscriptionCreated() =
        handleEvent<TechBlogSubscribeCreatedEvent>(defaultStream) { event ->
            techBlogRepository.incrementSubscriptionCount(event.techBlogId, +1)
        }

    @EventHandler
    fun subscriptionRemoved() =
        handleEvent<TechBlogSubscribeRemovedEvent>(defaultStream) { event ->
            techBlogRepository.incrementSubscriptionCount(event.techBlogId, -1)
        }
}
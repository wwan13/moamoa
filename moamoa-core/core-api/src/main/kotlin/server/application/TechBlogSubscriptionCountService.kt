package server.application

import org.springframework.stereotype.Service
import server.domain.techblog.TechBlogRepository
import server.domain.techblog.TechBlogSubscribeCreatedEvent
import server.domain.techblog.TechBlogSubscribeRemovedEvent
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
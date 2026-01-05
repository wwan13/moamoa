package server.application

import org.springframework.stereotype.Service
import server.domain.techblog.TechBlogRepository
import server.domain.techblog.TechBlogSubscribeCreatedEvent
import server.domain.techblog.TechBlogSubscribeRemovedEvent
import server.messaging.EventHandler
import server.messaging.handleEvent

@Service
class TechBlogSubscriptionCountService(
    private val techBlogRepository: TechBlogRepository
) {
    @EventHandler
    fun subscriptionCreated() = handleEvent<TechBlogSubscribeCreatedEvent> { event ->
        techBlogRepository.incrementSubscriptionCount(event.techBlogId, +1)
    }

    @EventHandler
    fun subscriptionRemoved() = handleEvent<TechBlogSubscribeRemovedEvent> { event ->
        techBlogRepository.incrementSubscriptionCount(event.techBlogId, -1)
    }
}
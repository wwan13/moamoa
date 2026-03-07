package server.core.feature.subscription.application

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import server.core.feature.subscription.domain.TechBlogSubscribeUpdatedEvent
import server.core.feature.techblog.domain.TechBlogRepository
import server.core.infra.db.transaction.Transactional
import server.messaging.handleMessage

@Service
class SubscriptionCountService(
    private val countProcessingStream: server.messaging.SubscriptionDefinition,
    private val techBlogRepository: TechBlogRepository,
    private val transactional: Transactional,
) {
    @server.messaging.EventHandler
    fun subscriptionUpdatedCountCalculate() =
        handleMessage<TechBlogSubscribeUpdatedEvent>(countProcessingStream) { event ->
            val delta = if (event.subscribed) +1L else -1L
            transactional {
                val techBlog = techBlogRepository.findByIdOrNull(event.techBlogId) ?: return@transactional
                techBlog.updateSubscriptionCount(delta)
            }
        }
}

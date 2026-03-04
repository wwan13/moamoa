package server.feature.techblogsubscription.application

import org.springframework.stereotype.Service
import server.feature.techblogsubscription.domain.TechBlogSubscribeUpdatedEvent
import server.infra.db.transaction.Transactional
import server.shared.messaging.EventHandler
import server.shared.messaging.SubscriptionDefinition
import server.shared.messaging.handleMessage
import org.springframework.data.repository.findByIdOrNull
import server.feature.techblog.command.domain.TechBlogRepository

@Service
class TechBlogSubscriptionCountService(
    private val countProcessingStream: SubscriptionDefinition,
    private val techBlogRepository: TechBlogRepository,
    private val transactional: Transactional,
) {
    @EventHandler
    fun subscriptionUpdatedCountCalculate() =
        handleMessage<TechBlogSubscribeUpdatedEvent>(countProcessingStream) { event ->
            val delta = if (event.subscribed) +1L else -1L
            transactional {
                val techBlog = techBlogRepository.findByIdOrNull(event.techBlogId) ?: return@transactional
                techBlog.updateSubscriptionCount(delta)
            }
        }
}

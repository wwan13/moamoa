package server.core.feature.techblogsubscription.application

import org.springframework.stereotype.Service
import server.core.feature.techblogsubscription.domain.TechBlogSubscribeUpdatedEvent
import server.core.infra.db.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import server.core.feature.techblog.domain.TechBlogRepository

@Service
class TechBlogSubscriptionCountService(
    private val countProcessingStream: server.messaging.SubscriptionDefinition,
    private val techBlogRepository: TechBlogRepository,
    private val transactional: Transactional,
) {
    @server.messaging.EventHandler
    fun subscriptionUpdatedCountCalculate() =
        _root_ide_package_.server.messaging.handleMessage<TechBlogSubscribeUpdatedEvent>(countProcessingStream) { event ->
            val delta = if (event.subscribed) +1L else -1L
            transactional {
                val techBlog = techBlogRepository.findByIdOrNull(event.techBlogId) ?: return@transactional
                techBlog.updateSubscriptionCount(delta)
            }
        }
}

package server.feature.techblog.command.application

import org.springframework.stereotype.Service
import server.feature.techblogsubscription.domain.NotificationUpdatedEvent
import server.feature.techblogsubscription.domain.TechBlogSubscribeUpdatedEvent
import server.infra.cache.TechBlogSubscriptionCache
import server.infra.cache.TechBlogSummaryCache
import server.shared.messaging.EventHandler
import server.shared.messaging.SubscriptionDefinition
import server.shared.messaging.handleMessage

@Service
class TechBlogCacheEvictService(
    private val techBlogCacheHandlingStream: SubscriptionDefinition,
    private val techBlogSummaryCache: TechBlogSummaryCache,
    private val techBlogSubscriptionCache: TechBlogSubscriptionCache
) {

    @EventHandler
    fun subscriptionUpdatedTechBlogCacheEvict() =
        handleMessage<TechBlogSubscribeUpdatedEvent>(techBlogCacheHandlingStream) { event ->
            techBlogSummaryCache.evict(event.techBlogId)
            techBlogSubscriptionCache.evictAll(event.memberId)
        }

    @EventHandler
    fun notificationUpdatedTechBlogCacheEvict() =
        handleMessage<NotificationUpdatedEvent>(techBlogCacheHandlingStream) { event ->
            techBlogSummaryCache.evict(event.techBlogId)
            techBlogSubscriptionCache.evictAll(event.memberId)
        }
}
package server.core.feature.techblog.application

import org.springframework.stereotype.Service
import server.core.feature.subscription.domain.NotificationUpdatedEvent
import server.core.feature.subscription.domain.TechBlogSubscribeUpdatedEvent
import server.core.feature.techblog.infra.SubscriptionCache
import server.core.feature.techblog.infra.TechBlogSummaryCache
import server.messaging.EventHandler
import server.messaging.SubscriptionDefinition
import server.messaging.handleMessage

@Service
class TechBlogCacheEvictService(
    private val techBlogCacheHandlingStream: SubscriptionDefinition,
    private val techBlogSummaryCache: TechBlogSummaryCache,
    private val subscriptionCache: SubscriptionCache
) {

    @EventHandler
    fun subscriptionUpdatedTechBlogCacheEvict() =
        handleMessage<TechBlogSubscribeUpdatedEvent>(techBlogCacheHandlingStream) { event ->
            techBlogSummaryCache.evict(event.techBlogId)
            subscriptionCache.evictAll(event.memberId)
        }

    @EventHandler
    fun notificationUpdatedTechBlogCacheEvict() =
        handleMessage<NotificationUpdatedEvent>(techBlogCacheHandlingStream) { event ->
            techBlogSummaryCache.evict(event.techBlogId)
            subscriptionCache.evictAll(event.memberId)
        }
}
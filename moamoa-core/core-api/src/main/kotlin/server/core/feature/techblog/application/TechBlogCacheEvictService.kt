package server.core.feature.techblog.application

import org.springframework.stereotype.Service
import server.core.feature.subscription.domain.NotificationUpdatedEvent
import server.core.feature.subscription.domain.TechBlogSubscribeUpdatedEvent
import server.core.infra.cache.SubscriptionCache
import server.core.infra.cache.TechBlogSummaryCache
import server.messaging.EventHandler
import server.messaging.SubscriptionDefinition

@Service
class TechBlogCacheEvictService(
    private val techBlogCacheHandlingStream: SubscriptionDefinition,
    private val techBlogSummaryCache: TechBlogSummaryCache,
    private val subscriptionCache: SubscriptionCache
) {

    @EventHandler
    fun subscriptionUpdatedTechBlogCacheEvict() =
        _root_ide_package_.server.messaging.handleMessage<TechBlogSubscribeUpdatedEvent>(techBlogCacheHandlingStream) { event ->
            techBlogSummaryCache.evict(event.techBlogId)
            subscriptionCache.evictAll(event.memberId)
        }

    @EventHandler
    fun notificationUpdatedTechBlogCacheEvict() =
        _root_ide_package_.server.messaging.handleMessage<NotificationUpdatedEvent>(techBlogCacheHandlingStream) { event ->
            techBlogSummaryCache.evict(event.techBlogId)
            subscriptionCache.evictAll(event.memberId)
        }
}
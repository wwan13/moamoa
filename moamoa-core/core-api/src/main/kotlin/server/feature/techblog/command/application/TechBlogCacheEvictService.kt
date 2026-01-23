package server.feature.techblog.command.application

import org.springframework.stereotype.Service
import server.feature.techblogsubscription.domain.NotificationUpdatedEvent
import server.feature.techblogsubscription.domain.TechBlogSubscribeUpdatedEvent
import server.infra.cache.TechBlogSubscriptionCache
import server.infra.cache.TechBlogSummaryCache
import server.messaging.EventHandler
import server.messaging.StreamDefinition
import server.messaging.handleEvent

@Service
class TechBlogCacheEvictService(
    private val techBlogCacheHandlingStream: StreamDefinition,
    private val techBlogSummaryCache: TechBlogSummaryCache,
    private val techBlogSubscriptionCache: TechBlogSubscriptionCache
) {

    @EventHandler
    fun subscriptionUpdatedTechBlogCacheEvict() =
        handleEvent<TechBlogSubscribeUpdatedEvent>(techBlogCacheHandlingStream) { event ->
            techBlogSummaryCache.evict(event.techBlogId)
            techBlogSubscriptionCache.evictAll(event.memberId)
        }

    @EventHandler
    fun notificationUpdatedTechBlogCacheEvict() =
        handleEvent<NotificationUpdatedEvent>(techBlogCacheHandlingStream) { event ->
            techBlogSummaryCache.evict(event.techBlogId)
            techBlogSubscriptionCache.evictAll(event.memberId)
        }
}
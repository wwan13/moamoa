package server.feature.techblog.command.application

import org.springframework.stereotype.Service
import server.feature.techblogsubscription.domain.NotificationDisabledEvent
import server.feature.techblogsubscription.domain.NotificationEnabledEvent
import server.feature.techblogsubscription.domain.TechBlogSubscribeCreatedEvent
import server.feature.techblogsubscription.domain.TechBlogSubscribeRemovedEvent
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
    fun subscriptionCreatedTechBlogCacheEvict() =
        handleEvent<TechBlogSubscribeCreatedEvent>(techBlogCacheHandlingStream) { event ->
            techBlogSummaryCache.evict(event.techBlogId)
            techBlogSubscriptionCache.evictAll(event.memberId)
        }

    @EventHandler
    fun subscriptionRemovedTechBlogCacheEvict() =
        handleEvent<TechBlogSubscribeRemovedEvent>(techBlogCacheHandlingStream) { event ->
            techBlogSummaryCache.evict(event.techBlogId)
            techBlogSubscriptionCache.evictAll(event.memberId)
        }

    @EventHandler
    fun notificationEnabledTechBlogCacheEvict() =
        handleEvent<NotificationEnabledEvent>(techBlogCacheHandlingStream) { event ->
            techBlogSummaryCache.evict(event.techBlogId)
            techBlogSubscriptionCache.evictAll(event.memberId)
        }

    @EventHandler
    fun notificationDisabledTechBlogCacheEvict() =
        handleEvent<NotificationDisabledEvent>(techBlogCacheHandlingStream) { event ->
            techBlogSummaryCache.evict(event.techBlogId)
            techBlogSubscriptionCache.evictAll(event.memberId)
        }
}
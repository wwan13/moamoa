package server.core.feature.techblog.application

import org.springframework.stereotype.Service
import server.core.feature.subscription.application.NotificationUpdatedEvent
import server.core.feature.subscription.application.TechBlogSubscribeUpdatedEvent
import server.core.feature.techblog.infra.SubscriptionCache
import server.core.feature.techblog.infra.TechBlogSummaryCache
import server.messaging.annotation.EventHandler
import server.messaging.definition.EventStream

@Service
class TechBlogCacheEvictService(
    private val techBlogSummaryCache: TechBlogSummaryCache,
    private val subscriptionCache: SubscriptionCache
) {

    @EventHandler(EventStream.TECH_BLOG_CACHE_HANDLING)
    fun subscriptionUpdatedTechBlogCacheEvict(event: TechBlogSubscribeUpdatedEvent) {
        techBlogSummaryCache.evict(event.techBlogId)
        subscriptionCache.evictAll(event.memberId)
    }

    @EventHandler(EventStream.TECH_BLOG_CACHE_HANDLING)
    fun notificationUpdatedTechBlogCacheEvict(event: NotificationUpdatedEvent) {
        techBlogSummaryCache.evict(event.techBlogId)
        subscriptionCache.evictAll(event.memberId)
    }
}

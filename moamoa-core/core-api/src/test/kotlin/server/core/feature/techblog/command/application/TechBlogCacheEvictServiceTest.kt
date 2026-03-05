package server.core.feature.techblog.command.application

import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import server.core.feature.techblog.application.TechBlogCacheEvictService
import server.core.feature.techblogsubscription.domain.NotificationUpdatedEvent
import server.core.feature.techblogsubscription.domain.TechBlogSubscribeUpdatedEvent
import server.core.infra.cache.TechBlogSubscriptionCache
import server.core.infra.cache.TechBlogSummaryCache
import test.UnitTest

class TechBlogCacheEvictServiceTest : UnitTest() {
    @Test
    fun `구독 정보가 변경되면 기술 블로그 캐시를 무효화한다`() = runTest {
        val techBlogCacheHandlingStream = mockk<server.messaging.SubscriptionDefinition>()
        val techBlogSummaryCache = mockk<TechBlogSummaryCache>(relaxed = true)
        val techBlogSubscriptionCache = mockk<TechBlogSubscriptionCache>(relaxed = true)
        val service = TechBlogCacheEvictService(
            techBlogCacheHandlingStream,
            techBlogSummaryCache,
            techBlogSubscriptionCache
        )
        val event = TechBlogSubscribeUpdatedEvent(memberId = 10L, techBlogId = 20L, subscribed = true)

        val handler = service.subscriptionUpdatedTechBlogCacheEvict()
        handler.handler(event)

        coVerify(exactly = 1) { techBlogSummaryCache.evict(event.techBlogId) }
        coVerify(exactly = 1) { techBlogSubscriptionCache.evictAll(event.memberId) }
    }

    @Test
    fun `구독 알림 정보가 변경되면 기술 블로그 캐시를 무효화한다`() = runTest {
        val techBlogCacheHandlingStream = mockk<server.messaging.SubscriptionDefinition>()
        val techBlogSummaryCache = mockk<TechBlogSummaryCache>(relaxed = true)
        val techBlogSubscriptionCache = mockk<TechBlogSubscriptionCache>(relaxed = true)
        val service = TechBlogCacheEvictService(
            techBlogCacheHandlingStream,
            techBlogSummaryCache,
            techBlogSubscriptionCache
        )
        val event = NotificationUpdatedEvent(memberId = 10L, techBlogId = 20L, enabled = true)

        val handler = service.notificationUpdatedTechBlogCacheEvict()
        handler.handler(event)

        coVerify(exactly = 1) { techBlogSummaryCache.evict(event.techBlogId) }
        coVerify(exactly = 1) { techBlogSubscriptionCache.evictAll(event.memberId) }
    }
}

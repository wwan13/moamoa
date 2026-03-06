package server.core.feature.techblog.query

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import server.core.feature.techblog.query.SubscribedTechBlogReader
import server.core.feature.techblog.query.SubscriptionInfo
import server.core.feature.techblog.infra.SubscriptionCache
import server.core.infra.cache.WarmupCoordinator
import test.UnitTest

class SubscribedTechBlogReaderTest : UnitTest() {
    @Test
    fun `캐시된 구독 목록이 있으면 캐시에서 필터링한다`() {
        val subscriptionCache = mockk<SubscriptionCache>()
        every { subscriptionCache.get(1L) } returns listOf(
            SubscriptionInfo(1L, subscribed = true, notificationEnabled = false),
            SubscriptionInfo(3L, subscribed = true, notificationEnabled = true)
        )

        val reader = SubscribedTechBlogReader(
            entityManager = mockk<EntityManager>(relaxed = true),
            subscriptionCache = subscriptionCache,
            warmupCoordinator = mockk<WarmupCoordinator>(relaxed = true)
        )

        val result = reader.findSubscribedMap(1L, listOf(1L, 2L, 3L))

        result.keys shouldBe setOf(1L, 3L)
    }
}

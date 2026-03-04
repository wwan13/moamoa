package server.feature.techblog.query

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import server.infra.cache.TechBlogSubscriptionCache
import server.infra.cache.WarmupCoordinator
import test.UnitTest

class SubscribedTechBlogReaderTest : UnitTest() {
    @Test
    fun `캐시된 구독 목록이 있으면 캐시에서 필터링한다`() {
        val techBlogSubscriptionCache = mockk<TechBlogSubscriptionCache>()
        every { techBlogSubscriptionCache.get(1L) } returns listOf(
            TechBlogSubscriptionInfo(1L, subscribed = true, notificationEnabled = false),
            TechBlogSubscriptionInfo(3L, subscribed = true, notificationEnabled = true)
        )

        val reader = SubscribedTechBlogReader(
            jdbc = mockk<NamedParameterJdbcTemplate>(),
            techBlogSubscriptionCache = techBlogSubscriptionCache,
            warmupCoordinator = mockk<WarmupCoordinator>(relaxed = true)
        )

        val result = reader.findSubscribedMap(1L, listOf(1L, 2L, 3L))

        result.keys shouldBe setOf(1L, 3L)
    }
}

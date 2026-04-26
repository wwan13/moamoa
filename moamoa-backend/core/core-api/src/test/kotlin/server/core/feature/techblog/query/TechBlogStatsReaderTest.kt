package server.core.feature.techblog.query

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import server.core.feature.techblog.query.TechBlogStats
import server.core.feature.techblog.query.TechBlogStatsReader
import server.core.feature.techblog.query.TechBlogSummary
import server.core.feature.techblog.infra.TechBlogSummaryCache
import server.core.infra.cache.WarmupCoordinator
import test.UnitTest

class TechBlogStatsReaderTest : UnitTest() {
    @Test
    fun `캐시된 통계가 있으면 캐시에서 조회한다`() {
        val techBlogSummaryCache = mockk<TechBlogSummaryCache>()
        every { techBlogSummaryCache.mGet(listOf(1L, 2L)) } returns mapOf(
            1L to techBlogSummary(id = 1L, subscriptionCount = 10L, postCount = 3L),
            2L to techBlogSummary(id = 2L, subscriptionCount = 5L, postCount = 1L)
        )

        val reader = TechBlogStatsReader(
            entityManager = mockk<EntityManager>(relaxed = true),
            techBlogSummaryCache = techBlogSummaryCache,
            warmupCoordinator = mockk<WarmupCoordinator>(relaxed = true)
        )

        val result = reader.findTechBlogStatsMap(listOf(1L, 2L))

        result shouldBe mapOf(
            1L to TechBlogStats(1L, 10L, 3L),
            2L to TechBlogStats(2L, 5L, 1L)
        )
    }

    private fun techBlogSummary(id: Long, subscriptionCount: Long, postCount: Long) = TechBlogSummary(
        id = id,
        title = "blog-$id",
        icon = "icon-$id",
        blogUrl = "https://blog.example.com/$id",
        key = "blog-key-$id",
        subscriptionCount = subscriptionCount,
        postCount = postCount,
        subscribed = false,
        notificationEnabled = false
    )
}

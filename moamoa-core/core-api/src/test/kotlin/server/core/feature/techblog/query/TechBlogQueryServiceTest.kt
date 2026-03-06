package server.core.feature.techblog.query

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import server.core.feature.member.domain.MemberRole
import server.core.feature.techblog.infra.TechBlogListCache
import server.core.infra.cache.WarmupCoordinator
import server.core.global.security.Passport
import test.UnitTest

class TechBlogQueryServiceTest : UnitTest() {
    @Test
    fun `캐시된 기술 블로그를 통계 및 구독정보와 병합한다`() {
        val entityManager = mockk<EntityManager>(relaxed = true)
        val techBlogListCache = mockk<TechBlogListCache>()
        val techBlogStatsReader = mockk<TechBlogStatsReader>()
        val subscribedTechBlogReader = mockk<SubscribedTechBlogReader>()

        val baseList = listOf(
            techBlogSummary(id = 1L, title = "blog-a"),
            techBlogSummary(id = 2L, title = "blog-b")
        )

        every { techBlogListCache.get() } returns baseList
        every { techBlogStatsReader.findTechBlogStatsMap(listOf(1L, 2L)) } returns mapOf(
            1L to TechBlogStats(techBlogId = 1L, subscriptionCount = 3L, postCount = 5L)
        )
        every { subscribedTechBlogReader.findSubscribedMap(1L, listOf(1L, 2L)) } returns mapOf(
            2L to SubscriptionInfo(2L, subscribed = true, notificationEnabled = true)
        )

        val service = TechBlogQueryService(
            entityManager,
            techBlogListCache,
            techBlogStatsReader,
            subscribedTechBlogReader,
            mockk<WarmupCoordinator>(relaxed = true)
        )

        val result = service.findAll(Passport(1L, MemberRole.USER), TechBlogQueryConditions(query = null))

        result.meta.totalCount shouldBe 2L
        result.techBlogs[0].subscriptionCount shouldBe 3L
        result.techBlogs[1].subscribed shouldBe true
    }

    private fun techBlogSummary(id: Long, title: String) = TechBlogSummary(
        id = id,
        title = title,
        icon = "icon-$id",
        blogUrl = "https://blog.example.com/$id",
        key = "blog-key-$id",
        subscriptionCount = 0L,
        postCount = 0L,
        subscribed = false,
        notificationEnabled = false
    )
}

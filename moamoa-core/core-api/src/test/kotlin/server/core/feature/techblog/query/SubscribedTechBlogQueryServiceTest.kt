package server.core.feature.techblog.query

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import server.core.feature.member.domain.MemberRole
import server.core.feature.techblog.infra.TechBlogSummaryCache
import server.core.infra.cache.WarmupCoordinator
import server.core.global.security.Passport
import test.UnitTest

class SubscribedTechBlogQueryServiceTest : UnitTest() {
    @Test
    fun `구독중인 기술 블로그가 없으면 빈 결과를 반환한다`() {
        val service = SubscribedTechBlogQueryService(
            entityManager = mockk<EntityManager>(relaxed = true),
            subscribedTechBlogReader = mockk<SubscribedTechBlogReader>().also {
                every { it.findAllSubscribedList(1L) } returns emptyList()
            },
            techBlogSummaryCache = mockk<TechBlogSummaryCache>(),
            warmupCoordinator = mockk<WarmupCoordinator>(relaxed = true)
        )

        val result = service.findSubscribingTechBlogs(Passport(1L, MemberRole.USER))

        result.meta.totalCount shouldBe 0L
        result.techBlogs shouldBe emptyList()
    }
}

package server.feature.techblog.query

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import server.feature.member.command.domain.MemberRole
import server.infra.cache.TechBlogSummaryCache
import server.infra.cache.WarmupCoordinator
import server.security.Passport
import test.UnitTest

class SubscribedTechBlogQueryServiceTest : UnitTest() {
    @Test
    fun `구독중인 기술 블로그가 없으면 빈 결과를 반환한다`() {
        val service = SubscribedTechBlogQueryService(
            jdbc = mockk<NamedParameterJdbcTemplate>(),
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

package server.core.feature.subscription.application

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import server.core.feature.subscription.domain.TechBlogSubscribeUpdatedEvent
import server.core.feature.techblog.domain.TechBlogRepository
import server.core.fixture.createTechBlog
import test.UnitTest

class SubscriptionCountServiceTest : UnitTest() {
    @Test
    fun `구독이 켜지면 카운트를 증가시킨다`() = runTest {
        val techBlogRepository = mockk<TechBlogRepository>(relaxed = true)
        val service = SubscriptionCountService(techBlogRepository)
        val techBlog = createTechBlog(id = 10L, subscriptionCount = 2L)
        every { techBlogRepository.findById(10L) } returns java.util.Optional.of(techBlog)

        val event = TechBlogSubscribeUpdatedEvent(memberId = 1L, techBlogId = 10L, subscribed = true)

        service.subscriptionUpdatedCountCalculate(event)

        techBlog.subscriptionCount shouldBe 3L
        verify(exactly = 1) { techBlogRepository.findById(event.techBlogId) }
    }

    @Test
    fun `구독이 해제되면 카운트를 감소시킨다`() = runTest {
        val techBlogRepository = mockk<TechBlogRepository>(relaxed = true)
        val service = SubscriptionCountService(techBlogRepository)
        val techBlog = createTechBlog(id = 10L, subscriptionCount = 1L)
        every { techBlogRepository.findById(10L) } returns java.util.Optional.of(techBlog)

        val event = TechBlogSubscribeUpdatedEvent(memberId = 1L, techBlogId = 10L, subscribed = false)

        service.subscriptionUpdatedCountCalculate(event)

        techBlog.subscriptionCount shouldBe 0L
        verify(exactly = 1) { techBlogRepository.findById(event.techBlogId) }
    }
}

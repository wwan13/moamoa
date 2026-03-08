package server.core.feature.subscription.application

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionStatus
import server.core.feature.subscription.domain.TechBlogSubscribeUpdatedEvent
import server.core.feature.techblog.domain.TechBlogRepository
import server.core.fixture.createTechBlog
import server.core.infra.db.transaction.HandleTransactionEvent
import server.messaging.MessageChannel
import server.messaging.SubscriptionDefinition
import test.UnitTest

class SubscriptionCountServiceTest : UnitTest() {
    @Test
    fun `이벤트 핸들러는 스트림과 타입 정보를 포함한다`() {
        val stream = SubscriptionDefinition(
            MessageChannel("tech-blog-subscription"), "tech-blog-subscription-group"
        )
        val techBlogRepository = mockk<TechBlogRepository>(relaxed = true)
        val service = SubscriptionCountService(
            stream,
            techBlogRepository,
            HandleTransactionEvent(newTxManager())
        )

        val handler = service.subscriptionUpdatedCountCalculate()

        handler.subscription shouldBe stream
        handler.type shouldBe TechBlogSubscribeUpdatedEvent::class.java.simpleName
        handler.payloadClass shouldBe TechBlogSubscribeUpdatedEvent::class.java
    }

    @Test
    fun `구독이 켜지면 카운트를 증가시킨다`() = runTest {
        val stream = SubscriptionDefinition(
            MessageChannel("tech-blog-subscription"), "tech-blog-subscription-group"
        )
        val techBlogRepository = mockk<TechBlogRepository>(relaxed = true)
        val service = SubscriptionCountService(
            stream,
            techBlogRepository,
            HandleTransactionEvent(newTxManager())
        )
        val techBlog = createTechBlog(id = 10L, subscriptionCount = 2L)
        every { techBlogRepository.findById(10L) } returns java.util.Optional.of(techBlog)

        val handler = service.subscriptionUpdatedCountCalculate()
        val event = TechBlogSubscribeUpdatedEvent(memberId = 1L, techBlogId = 10L, subscribed = true)

        handler.handler(event)

        techBlog.subscriptionCount shouldBe 3L
        verify(exactly = 1) { techBlogRepository.findById(event.techBlogId) }
    }

    @Test
    fun `구독이 해제되면 카운트를 감소시킨다`() = runTest {
        val stream = SubscriptionDefinition(
            MessageChannel("tech-blog-subscription"), "tech-blog-subscription-group"
        )
        val techBlogRepository = mockk<TechBlogRepository>(relaxed = true)
        val service = SubscriptionCountService(
            stream,
            techBlogRepository,
            HandleTransactionEvent(newTxManager())
        )
        val techBlog = createTechBlog(id = 10L, subscriptionCount = 1L)
        every { techBlogRepository.findById(10L) } returns java.util.Optional.of(techBlog)

        val handler = service.subscriptionUpdatedCountCalculate()
        val event = TechBlogSubscribeUpdatedEvent(memberId = 1L, techBlogId = 10L, subscribed = false)

        handler.handler(event)

        techBlog.subscriptionCount shouldBe 0L
        verify(exactly = 1) { techBlogRepository.findById(event.techBlogId) }
    }

    private fun newTxManager(): PlatformTransactionManager {
        val txManager = mockk<PlatformTransactionManager>()
        val status = mockk<TransactionStatus>(relaxed = true)
        every { txManager.getTransaction(any()) } returns status
        every { txManager.commit(status) } just runs
        every { txManager.rollback(status) } just runs
        return txManager
    }
}

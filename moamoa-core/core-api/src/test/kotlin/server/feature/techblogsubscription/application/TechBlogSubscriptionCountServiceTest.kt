package server.feature.techblogsubscription.application

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import server.feature.techblog.command.domain.TechBlogRepository
import server.feature.techblogsubscription.domain.TechBlogSubscribeUpdatedEvent
import server.fixture.createTechBlog
import server.infra.db.transaction.TransactionScope
import server.infra.db.transaction.Transactional
import server.shared.messaging.MessageChannel
import server.shared.messaging.SubscriptionDefinition
import test.UnitTest

class TechBlogSubscriptionCountServiceTest : UnitTest() {
    @Test
    fun `이벤트 핸들러는 스트림과 타입 정보를 포함한다`() {
        val stream = SubscriptionDefinition(MessageChannel("tech-blog-subscription"), "tech-blog-subscription-group")
        val techBlogRepository = mockk<TechBlogRepository>(relaxed = true)
        val transactional = mockk<Transactional>(relaxed = true)
        val service = TechBlogSubscriptionCountService(stream, techBlogRepository, transactional)

        val handler = service.subscriptionUpdatedCountCalculate()

        handler.subscription shouldBe stream
        handler.type shouldBe TechBlogSubscribeUpdatedEvent::class.java.simpleName
        handler.payloadClass shouldBe TechBlogSubscribeUpdatedEvent::class.java
    }

    @Test
    fun `구독이 켜지면 카운트를 증가시킨다`() = runTest {
        val stream = SubscriptionDefinition(MessageChannel("tech-blog-subscription"), "tech-blog-subscription-group")
        val techBlogRepository = mockk<TechBlogRepository>(relaxed = true)
        val transactional = mockk<Transactional>()
        val transactionScope = mockk<TransactionScope>(relaxed = true)
        val service = TechBlogSubscriptionCountService(stream, techBlogRepository, transactional)
        val techBlog = createTechBlog(id = 10L, subscriptionCount = 2L)
        every { techBlogRepository.findById(10L) } returns java.util.Optional.of(techBlog)
        every { transactional.invoke<Unit>(any(), any()) } answers {
            val block = secondArg<TransactionScope.() -> Unit>()
            block.invoke(transactionScope)
        }

        val handler = service.subscriptionUpdatedCountCalculate()
        val event = TechBlogSubscribeUpdatedEvent(memberId = 1L, techBlogId = 10L, subscribed = true)

        handler.handler(event)

        techBlog.subscriptionCount shouldBe 3L
        verify(exactly = 1) { techBlogRepository.findById(event.techBlogId) }
    }

    @Test
    fun `구독이 해제되면 카운트를 감소시킨다`() = runTest {
        val stream = SubscriptionDefinition(MessageChannel("tech-blog-subscription"), "tech-blog-subscription-group")
        val techBlogRepository = mockk<TechBlogRepository>(relaxed = true)
        val transactional = mockk<Transactional>()
        val transactionScope = mockk<TransactionScope>(relaxed = true)
        val service = TechBlogSubscriptionCountService(stream, techBlogRepository, transactional)
        val techBlog = createTechBlog(id = 10L, subscriptionCount = 1L)
        every { techBlogRepository.findById(10L) } returns java.util.Optional.of(techBlog)
        every { transactional.invoke<Unit>(any(), any()) } answers {
            val block = secondArg<TransactionScope.() -> Unit>()
            block.invoke(transactionScope)
        }

        val handler = service.subscriptionUpdatedCountCalculate()
        val event = TechBlogSubscribeUpdatedEvent(memberId = 1L, techBlogId = 10L, subscribed = false)

        handler.handler(event)

        techBlog.subscriptionCount shouldBe 0L
        verify(exactly = 1) { techBlogRepository.findById(event.techBlogId) }
    }
}

package server.core.infra.messagebroker

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionStatus
import server.core.infra.outbox.EventOutbox
import server.core.infra.outbox.EventOutboxRepository
import server.core.infra.outbox.OutboxEventDispatcher
import server.messaging.EventPublisher
import server.messaging.health.MessagingHealthStateManager
import test.UnitTest

class OutboxEventDispatcherTest : UnitTest() {
    @Test
    fun `미발행 이벤트가 없으면 아무 작업도 하지 않는다`() = runTest {
        val brokerEventPublisher = mockk<EventPublisher>()
        val eventOutboxRepository = mockk<EventOutboxRepository>()
        val txManager = newTxManager()
        coEvery { eventOutboxRepository.findUnpublished(10) } returns emptyList()
        val healthStateManager = newHealthStateManager()
        val dispatcher = OutboxEventDispatcher(
            brokerEventPublisher = brokerEventPublisher,
            eventOutboxRepository = eventOutboxRepository,
            txManager = txManager,
            healthStateManager = healthStateManager,
        )

        dispatcher.dispatchBatch(10)

        verify(exactly = 0) { brokerEventPublisher.publish(any<String>(), any<String>(), any<String>(), any<String>()) }
        verify(exactly = 0) { eventOutboxRepository.findById(any()) }
    }

    @Test
    fun `미발행 이벤트가 있으면 발행 후 published로 마킹한다`() = runTest {
        val brokerEventPublisher = mockk<EventPublisher>()
        val eventOutboxRepository = mockk<EventOutboxRepository>()
        val txManager = newTxManager()
        val rows = listOf(
            EventOutbox(id = 1L, topic = "topic-1", type = "type-1", eventId = "e1", payload = "payload-1"),
            EventOutbox(id = 2L, topic = "topic-2", type = "type-2", eventId = "e2", payload = "payload-2"),
        )
        coEvery { eventOutboxRepository.findUnpublished(5) } returns rows
        every { brokerEventPublisher.publish(any<String>(), any<String>(), any<String>(), any<String>()) } just runs
        every { eventOutboxRepository.findById(1L) } returns java.util.Optional.of(rows[0])
        every { eventOutboxRepository.findById(2L) } returns java.util.Optional.of(rows[1])
        val healthStateManager = newHealthStateManager()
        val dispatcher = OutboxEventDispatcher(
            brokerEventPublisher = brokerEventPublisher,
            eventOutboxRepository = eventOutboxRepository,
            txManager = txManager,
            healthStateManager = healthStateManager,
        )

        dispatcher.dispatchBatch(5)

        verify(exactly = 1) { brokerEventPublisher.publish("topic-1", "type-1", "payload-1", "e1") }
        verify(exactly = 1) { brokerEventPublisher.publish("topic-2", "type-2", "payload-2", "e2") }
        rows[0].published shouldBe true
        rows[1].published shouldBe true
        verify(exactly = 1) { eventOutboxRepository.findById(1L) }
        verify(exactly = 1) { eventOutboxRepository.findById(2L) }
    }

    @Test
    fun `발행 실패한 이벤트는 마킹하지 않고 다음 이벤트를 처리한다`() = runTest {
        val brokerEventPublisher = mockk<EventPublisher>()
        val eventOutboxRepository = mockk<EventOutboxRepository>()
        val txManager = newTxManager()
        val rows = listOf(
            EventOutbox(id = 1L, topic = "topic-1", type = "type-1", eventId = "e1", payload = "payload-1"),
            EventOutbox(id = 2L, topic = "topic-2", type = "type-2", eventId = "e2", payload = "payload-2"),
        )
        coEvery { eventOutboxRepository.findUnpublished(2) } returns rows
        every { brokerEventPublisher.publish("topic-1", "type-1", "payload-1", "e1") } throws RuntimeException("fail")
        every { brokerEventPublisher.publish("topic-2", "type-2", "payload-2", "e2") } just runs
        every { eventOutboxRepository.findById(2L) } returns java.util.Optional.of(rows[1])
        val healthStateManager = newHealthStateManager()
        val dispatcher = OutboxEventDispatcher(
            brokerEventPublisher = brokerEventPublisher,
            eventOutboxRepository = eventOutboxRepository,
            txManager = txManager,
            healthStateManager = healthStateManager,
        )

        dispatcher.dispatchBatch(2)

        verify(exactly = 1) { brokerEventPublisher.publish("topic-1", "type-1", "payload-1", "e1") }
        verify(exactly = 1) { brokerEventPublisher.publish("topic-2", "type-2", "payload-2", "e2") }
        rows[0].published shouldBe false
        rows[1].published shouldBe true
        verify(exactly = 0) { eventOutboxRepository.findById(1L) }
        verify(exactly = 1) { eventOutboxRepository.findById(2L) }
    }

    private fun newTxManager(): PlatformTransactionManager {
        val txManager = mockk<PlatformTransactionManager>()
        val status = mockk<TransactionStatus>(relaxed = true)
        every { txManager.getTransaction(any()) } returns status
        every { txManager.commit(status) } just runs
        every { txManager.rollback(status) } just runs
        return txManager
    }

    private fun newHealthStateManager(): MessagingHealthStateManager {
        return mockk<MessagingHealthStateManager>(relaxed = true).also { manager ->
            every { manager.isDegraded() } returns false
            every { manager.tryRecover() } returns true
            every { manager.runSafe<Unit>(any()) } answers {
                firstArg<() -> Unit>().invoke()
                Result.success(Unit)
            }
            every { manager.isFailure(any()) } returns false
        }
    }
}

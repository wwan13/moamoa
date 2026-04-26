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
import server.core.infra.outbox.EventOutbox
import server.core.infra.outbox.EventOutboxRepository
import server.core.infra.outbox.OutboxEventDispatcher
import server.core.infra.outbox.OutboxEventMarker
import server.messaging.EventPublisher
import server.messaging.health.MessagingHealthChecker
import test.UnitTest

class OutboxEventDispatcherTest : UnitTest() {
    @Test
    fun `미발행 이벤트가 없으면 아무 작업도 하지 않는다`() = runTest {
        val brokerEventPublisher = mockk<EventPublisher>()
        val eventOutboxRepository = mockk<EventOutboxRepository>()
        val outboxEventMarker = mockk<OutboxEventMarker>(relaxed = true)
        val messagingHealthChecker = newMessagingHealthChecker()
        coEvery { eventOutboxRepository.findUnpublished(10) } returns emptyList()
        val dispatcher = OutboxEventDispatcher(
            eventPublisher = brokerEventPublisher,
            eventOutboxRepository = eventOutboxRepository,
            outboxEventMarker = outboxEventMarker,
            messagingHealthChecker = messagingHealthChecker,
        )

        dispatcher.dispatchBatch(10)

        verify(exactly = 0) { brokerEventPublisher.publish(any<String>(), any<String>(), any<String>(), any<String>()) }
        verify(exactly = 0) { outboxEventMarker.markPublished(any()) }
    }

    @Test
    fun `미발행 이벤트가 있으면 발행 후 published로 마킹한다`() = runTest {
        val brokerEventPublisher = mockk<EventPublisher>()
        val eventOutboxRepository = mockk<EventOutboxRepository>()
        val outboxEventMarker = mockk<OutboxEventMarker>(relaxed = true)
        val messagingHealthChecker = newMessagingHealthChecker()
        val rows = listOf(
            EventOutbox(id = 1L, topic = "topic-1", type = "type-1", eventId = "e1", payload = "payload-1"),
            EventOutbox(id = 2L, topic = "topic-2", type = "type-2", eventId = "e2", payload = "payload-2"),
        )
        coEvery { eventOutboxRepository.findUnpublished(5) } returns rows
        every { brokerEventPublisher.publish(any<String>(), any<String>(), any<String>(), any<String>()) } just runs
        val dispatcher = OutboxEventDispatcher(
            eventPublisher = brokerEventPublisher,
            eventOutboxRepository = eventOutboxRepository,
            outboxEventMarker = outboxEventMarker,
            messagingHealthChecker = messagingHealthChecker,
        )

        dispatcher.dispatchBatch(5)

        verify(exactly = 1) { brokerEventPublisher.publish("topic-1", "type-1", "payload-1", "e1") }
        verify(exactly = 1) { brokerEventPublisher.publish("topic-2", "type-2", "payload-2", "e2") }
        verify(exactly = 1) { outboxEventMarker.markPublished(listOf(1L, 2L)) }
    }

    @Test
    fun `발행 실패한 이벤트는 마킹하지 않고 다음 이벤트를 처리한다`() = runTest {
        val brokerEventPublisher = mockk<EventPublisher>()
        val eventOutboxRepository = mockk<EventOutboxRepository>()
        val outboxEventMarker = mockk<OutboxEventMarker>(relaxed = true)
        val messagingHealthChecker = newMessagingHealthChecker()
        val rows = listOf(
            EventOutbox(id = 1L, topic = "topic-1", type = "type-1", eventId = "e1", payload = "payload-1"),
            EventOutbox(id = 2L, topic = "topic-2", type = "type-2", eventId = "e2", payload = "payload-2"),
        )
        coEvery { eventOutboxRepository.findUnpublished(2) } returns rows
        every { brokerEventPublisher.publish("topic-1", "type-1", "payload-1", "e1") } throws RuntimeException("fail")
        every { brokerEventPublisher.publish("topic-2", "type-2", "payload-2", "e2") } just runs
        val dispatcher = OutboxEventDispatcher(
            eventPublisher = brokerEventPublisher,
            eventOutboxRepository = eventOutboxRepository,
            outboxEventMarker = outboxEventMarker,
            messagingHealthChecker = messagingHealthChecker,
        )

        dispatcher.dispatchBatch(2)

        verify(exactly = 1) { brokerEventPublisher.publish("topic-1", "type-1", "payload-1", "e1") }
        verify(exactly = 1) { brokerEventPublisher.publish("topic-2", "type-2", "payload-2", "e2") }
        verify(exactly = 1) { outboxEventMarker.markPublished(listOf(2L)) }
    }

    @Test
    fun `브로커 장애면 즉시 중단하고 누적 성공건만 마킹한다`() = runTest {
        val brokerEventPublisher = mockk<EventPublisher>()
        val eventOutboxRepository = mockk<EventOutboxRepository>()
        val outboxEventMarker = mockk<OutboxEventMarker>(relaxed = true)
        val messagingHealthChecker = newMessagingHealthChecker()
        val rows = listOf(
            EventOutbox(id = 1L, topic = "topic-1", type = "type-1", eventId = "e1", payload = "payload-1"),
            EventOutbox(id = 2L, topic = "topic-2", type = "type-2", eventId = "e2", payload = "payload-2"),
        )
        coEvery { eventOutboxRepository.findUnpublished(2) } returns rows
        every { brokerEventPublisher.publish("topic-1", "type-1", "payload-1", "e1") } throws
            IllegalStateException("redis down")

        val dispatcher = OutboxEventDispatcher(
            eventPublisher = brokerEventPublisher,
            eventOutboxRepository = eventOutboxRepository,
            outboxEventMarker = outboxEventMarker,
            messagingHealthChecker = messagingHealthChecker,
        )

        val result = dispatcher.dispatchBatch(2)

        result shouldBe false
        verify(exactly = 1) { brokerEventPublisher.publish("topic-1", "type-1", "payload-1", "e1") }
        verify(exactly = 0) { brokerEventPublisher.publish("topic-2", "type-2", "payload-2", "e2") }
        verify(exactly = 1) { outboxEventMarker.markPublished(emptyList()) }
    }

    @Test
    fun `헬시하지 않으면 복구를 시도하고 실패 시 조회 전에 중단한다`() = runTest {
        val brokerEventPublisher = mockk<EventPublisher>()
        val eventOutboxRepository = mockk<EventOutboxRepository>()
        val outboxEventMarker = mockk<OutboxEventMarker>(relaxed = true)
        val messagingHealthChecker = newMessagingHealthChecker(
            isHealthy = false,
            tryRecover = false,
        )
        val dispatcher = OutboxEventDispatcher(
            eventPublisher = brokerEventPublisher,
            eventOutboxRepository = eventOutboxRepository,
            outboxEventMarker = outboxEventMarker,
            messagingHealthChecker = messagingHealthChecker,
        )

        val result = dispatcher.dispatchBatch(10)

        result shouldBe false
        verify(exactly = 0) { eventOutboxRepository.findUnpublished(any()) }
        verify(exactly = 0) { brokerEventPublisher.publish(any<String>(), any<String>(), any<String>(), any<String>()) }
        verify(exactly = 0) { outboxEventMarker.markPublished(any()) }
    }

    private fun newMessagingHealthChecker(
        isHealthy: Boolean = true,
        tryRecover: Boolean = true,
    ): MessagingHealthChecker {
        return mockk<MessagingHealthChecker>().also { checker ->
            every { checker.isHealthy() } returns isHealthy
            every { checker.tryRecover() } returns tryRecover
            every { checker.healthCheck() } answers {
                if (checker.isHealthy()) true else checker.tryRecover()
            }
        }
    }
}

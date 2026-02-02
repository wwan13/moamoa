package server.infra.messagebroker

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import server.infra.db.outbox.EventOutbox
import server.infra.db.outbox.EventOutboxRepository
import server.messaging.StreamEventPublisher
import test.UnitTest

class OutboxPublishWorkerTest : UnitTest() {
    @Test
    fun `미발행 이벤트가 없으면 아무 작업도 하지 않는다`() = runTest {
        val eventPublisher = mockk<StreamEventPublisher>()
        val eventOutboxRepository = mockk<EventOutboxRepository>()
        coEvery { eventOutboxRepository.findUnpublished(10) } returns emptyList()
        val worker = OutboxPublishWorker(eventPublisher, eventOutboxRepository)

        worker.runOnce(10)

        verify(exactly = 0) { eventPublisher.publish(any<String>(), any<String>(), any<String>()) }
        coVerify(exactly = 0) { eventOutboxRepository.markPublished(any()) }
    }

    @Test
    fun `미발행 이벤트가 있으면 발행 후 published로 마킹한다`() = runTest {
        val eventPublisher = mockk<StreamEventPublisher>()
        val eventOutboxRepository = mockk<EventOutboxRepository>()
        val rows = listOf(
            EventOutbox(id = 1L, topic = "topic-1", type = "type-1", payload = "payload-1"),
            EventOutbox(id = 2L, topic = "topic-2", type = "type-2", payload = "payload-2"),
        )
        coEvery { eventOutboxRepository.findUnpublished(5) } returns rows
        every { eventPublisher.publish(any<String>(), any<String>(), any<String>()) } just runs
        coEvery { eventOutboxRepository.markPublished(any()) } returns 1
        val worker = OutboxPublishWorker(eventPublisher, eventOutboxRepository)

        worker.runOnce(5)

        verify(exactly = 1) { eventPublisher.publish("topic-1", "type-1", "payload-1") }
        verify(exactly = 1) { eventPublisher.publish("topic-2", "type-2", "payload-2") }
        coVerify(exactly = 1) { eventOutboxRepository.markPublished(1L) }
        coVerify(exactly = 1) { eventOutboxRepository.markPublished(2L) }
    }

    @Test
    fun `발행 실패한 이벤트는 마킹하지 않고 다음 이벤트를 처리한다`() = runTest {
        val eventPublisher = mockk<StreamEventPublisher>()
        val eventOutboxRepository = mockk<EventOutboxRepository>()
        val rows = listOf(
            EventOutbox(id = 1L, topic = "topic-1", type = "type-1", payload = "payload-1"),
            EventOutbox(id = 2L, topic = "topic-2", type = "type-2", payload = "payload-2"),
        )
        coEvery { eventOutboxRepository.findUnpublished(2) } returns rows
        every { eventPublisher.publish("topic-1", "type-1", "payload-1") } throws RuntimeException("fail")
        every { eventPublisher.publish("topic-2", "type-2", "payload-2") } just runs
        coEvery { eventOutboxRepository.markPublished(2L) } returns 1
        val worker = OutboxPublishWorker(eventPublisher, eventOutboxRepository)

        worker.runOnce(2)

        verify(exactly = 1) { eventPublisher.publish("topic-1", "type-1", "payload-1") }
        verify(exactly = 1) { eventPublisher.publish("topic-2", "type-2", "payload-2") }
        coVerify(exactly = 0) { eventOutboxRepository.markPublished(1L) }
        coVerify(exactly = 1) { eventOutboxRepository.markPublished(2L) }
    }
}

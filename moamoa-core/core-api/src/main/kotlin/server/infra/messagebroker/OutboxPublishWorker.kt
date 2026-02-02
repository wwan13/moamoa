package server.infra.messagebroker

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import server.infra.db.outbox.EventOutboxRepository
import server.messaging.StreamEventPublisher

@Component
class OutboxPublishWorker(
    private val eventPublisher: StreamEventPublisher,
    private val eventOutboxRepository: EventOutboxRepository,
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    suspend fun runOnce(batchSize: Int) {
        val rows = eventOutboxRepository.findUnpublished(batchSize)
        if (rows.isEmpty()) return

        for (row in rows) {
            try {
                eventPublisher.publish(row.topic, row.type, row.payload)
                eventOutboxRepository.markPublished(row.id)
            } catch (e: Exception) {
                log.warn("Outbox publish failed: id={}, topic={}", row.id, row.topic, e)
            }
        }
    }
}
package server.infra.db.outbox

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import server.messaging.StreamEventPublisher

@Component
class OutboxEventPublisher(
    private val eventPublisher: StreamEventPublisher,
    private val eventOutboxRepository: EventOutboxRepository,
    private val outboxScope: CoroutineScope,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 1000L)
    fun tick() {
        outboxScope.launch {
            publishPending(batchSize = 200)
        }
    }

    private suspend fun publishPending(batchSize: Int) {
        val rows = eventOutboxRepository.findUnpublished(batchSize)
        if (rows.isEmpty()) return

        for (row in rows) {
            try {
                eventPublisher.publish(row.topic, row.type, row.payload)
                val updated = eventOutboxRepository.markPublished(row.id) ?: 0

                if (updated == 0) {
                    log.debug("Outbox already published: id={}", row.id)
                }
            } catch (e: Exception) {
                log.warn("Outbox publish failed: id={}, topic={}", row.id, row.topic, e)
            }
        }
    }
}
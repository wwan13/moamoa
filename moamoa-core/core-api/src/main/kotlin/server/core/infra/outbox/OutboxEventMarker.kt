package server.core.infra.outbox

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class OutboxEventMarker(
    private val eventOutboxRepository: EventOutboxRepository,
) {

    @Transactional
    fun markPublished(outboxIds: List<Long>) {
        if (outboxIds.isEmpty()) return
        eventOutboxRepository.markPublishedByIds(outboxIds.distinct())
    }
}

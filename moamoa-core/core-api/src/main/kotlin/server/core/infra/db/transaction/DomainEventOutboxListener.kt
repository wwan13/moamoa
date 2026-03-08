package server.core.infra.db.transaction

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import server.core.support.domain.DomainEvent
import server.core.infra.db.outbox.EventOutbox
import server.core.infra.db.outbox.EventOutboxRepository
import server.messaging.MessageChannel

@Component
class DomainEventOutboxListener(
    private val eventOutboxRepository: EventOutboxRepository,
    private val defaultTopic: MessageChannel,
    private val objectMapper: ObjectMapper
) {

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun handle(event: DomainEvent) {
        val outbox = EventOutbox(
            topic = defaultTopic.key,
            type = event::class.simpleName!!,
            payload = objectMapper.writeValueAsString(event),
            published = false,
        )
        eventOutboxRepository.save(outbox)
    }
}

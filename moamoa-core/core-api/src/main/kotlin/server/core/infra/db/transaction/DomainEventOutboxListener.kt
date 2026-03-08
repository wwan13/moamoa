package server.core.infra.db.transaction

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import server.core.support.domain.DomainEvent
import server.core.infra.db.outbox.EventOutbox
import server.core.infra.db.outbox.EventOutboxRepository
import server.global.logging.event
import server.messaging.MessageChannel

@Component
class DomainEventOutboxListener(
    private val eventOutboxRepository: EventOutboxRepository,
    private val defaultTopic: MessageChannel,
    private val objectMapper: ObjectMapper
) {

    private val logger = KotlinLogging.logger {}

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun handle(event: DomainEvent) {
        logger.event.info(
            event,
            "topic" to defaultTopic.key,
            "type" to event::class.simpleName,
        ) { "도메인 이벤트를 outbox에 저장합니다" }

        val outbox = EventOutbox(
            topic = defaultTopic.key,
            type = event::class.simpleName!!,
            payload = objectMapper.writeValueAsString(event),
            published = false,
        )
        eventOutboxRepository.save(outbox)
    }
}

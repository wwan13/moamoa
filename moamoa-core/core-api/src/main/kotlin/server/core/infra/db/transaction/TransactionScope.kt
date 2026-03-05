package server.core.infra.db.transaction

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import server.core.infra.db.outbox.EventOutbox
import server.core.infra.db.outbox.EventOutboxRepository
import server.messaging.MessageChannel

@Component
class TransactionScope(
    private val eventOutboxRepository: EventOutboxRepository,
    private val defaultTopic: server.messaging.MessageChannel,
    private val objectMapper: ObjectMapper
) {

    fun registerEvent(
        event: Any,
        topic: server.messaging.MessageChannel = defaultTopic
    ) {
        val outbox = EventOutbox(
            topic = topic.key,
            type = event::class.simpleName!!,
            payload = objectMapper.writeValueAsString(event),
            published = false,
        )

        eventOutboxRepository.save(outbox)
    }
}
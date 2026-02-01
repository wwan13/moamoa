package server.infra.db.transaction

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import server.infra.db.outbox.EventOutbox
import server.infra.db.outbox.EventOutboxRepository
import server.messaging.StreamTopic

@Component
class TransactionScope(
    private val eventOutboxRepository: EventOutboxRepository,
    private val defaultTopic: StreamTopic,
    private val objectMapper: ObjectMapper
) {

    suspend fun registerEvent(
        event: Any,
        topic: StreamTopic = defaultTopic
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
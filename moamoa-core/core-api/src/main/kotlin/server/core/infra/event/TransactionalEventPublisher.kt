package server.core.infra.event

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import server.core.infra.db.outbox.EventOutbox
import server.core.infra.db.outbox.EventOutboxRepository
import server.global.logging.RequestLogContextHolder
import server.global.logging.event
import server.messaging.Event
import server.messaging.definition.EventChannel
import java.util.*

@Component
class TransactionalEventPublisher(
    private val eventOutboxRepository: EventOutboxRepository,
    private val objectMapper: ObjectMapper,
) {

    private val logger = KotlinLogging.logger {}

    @Transactional(propagation = Propagation.MANDATORY)
    fun publish(
        event: Event,
        channel: EventChannel = EventChannel.DEFAULT,
    ) {
        val eventId = generateEventId()
        val topic = channel.key

        logger.event.info(
            event,
            "topic" to topic,
            "type" to event.type,
            "eventId" to eventId,
        ) { "이벤트를 outbox에 저장합니다" }

        val outbox = EventOutbox(
            topic = topic,
            type = event.type,
            eventId = eventId,
            payload = objectMapper.writeValueAsString(event),
            published = false,
        )
        eventOutboxRepository.save(outbox)
    }

    private fun generateEventId(): String {
        val traceId = RequestLogContextHolder.current()?.traceId ?: RequestLogContextHolder.SYSTEM_TRACE_ID
        val suffix = UUID.randomUUID().toString().replace("-", "").take(8)
        return "$traceId-$suffix"
    }
}

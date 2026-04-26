package server.core.infra.outbox

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import server.global.logging.errorType
import server.messaging.EventPublisher
import server.messaging.health.MessagingHealthChecker

@Component
class OutboxEventDispatcher(
    private val eventPublisher: EventPublisher,
    private val eventOutboxRepository: EventOutboxRepository,
    private val outboxEventMarker: OutboxEventMarker,
    private val messagingHealthChecker: MessagingHealthChecker,
) {
    private val logger = KotlinLogging.logger {}

    fun dispatchBatch(batchSize: Int): Boolean {
        if (!messagingHealthChecker.healthCheck()) {
            return false
        }

        val rows = eventOutboxRepository.findUnpublished(batchSize)
        if (rows.isEmpty()) return true

        val publishedIds = rows.mapNotNull {
            publishEvent(it)
        }
        outboxEventMarker.markPublished(publishedIds)
        return true
    }

    private fun publishEvent(row: EventOutbox): Long? {
        return runCatching {
            val payloadJson = row.payload
            eventPublisher.publish(
                channel = row.topic,
                type = row.type,
                payloadJson = payloadJson,
                eventId = row.eventId,
            )
            row.id
        }.onFailure { e ->
            logger.errorType.warn(
                traceId = null,
                throwable = e,
                "call" to "EventPublisher.publish",
                "errorType" to (e::class.simpleName ?: "UnknownException"),
                "message" to "outboxId=${row.id} topic=${row.topic}",
            ) {
                "아웃박스 이벤트 발행 중 오류가 발생했습니다"
            }
        }.getOrNull()
    }
}

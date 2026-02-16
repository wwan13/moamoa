package server.infra.messagebroker

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import server.global.logging.ExternalCallLogger
import server.global.logging.warnWithTraceId
import server.infra.db.outbox.EventOutboxRepository
import server.messaging.StreamEventPublisher

@Component
class OutboxPublishWorker(
    private val eventPublisher: StreamEventPublisher,
    private val eventOutboxRepository: EventOutboxRepository,
    private val externalCallLogger: ExternalCallLogger,
) {
    private val logger = KotlinLogging.logger {}

    suspend fun runOnce(batchSize: Int) {
        val rows = eventOutboxRepository.findUnpublished(batchSize)
        if (rows.isEmpty()) return

        for (row in rows) {
            try {
                externalCallLogger.execute(
                    call = "StreamEventPublisher.publish",
                    target = "MQ",
                    retry = 0,
                    timeout = false
                ) {
                    eventPublisher.publish(row.topic, row.type, row.payload)
                }
                eventOutboxRepository.markPublished(row.id)
            } catch (e: Exception) {
                logger.warnWithTraceId(traceId = null, throwable = e) {
                    "[WORKER] result=FAIL call=StreamEventPublisher.publish target=MQ outboxId=${row.id} topic=${row.topic} errorCode=${e::class.simpleName ?: "UnknownException"}"
                }
            }
        }
    }
}

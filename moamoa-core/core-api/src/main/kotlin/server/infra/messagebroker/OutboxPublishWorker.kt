package server.infra.messagebroker

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import server.global.logging.ExternalCallLogger
import server.global.logging.errorType
import server.infra.db.outbox.EventOutboxRepository
import server.messaging.health.RedisHealthStateManager
import server.shared.messaging.EventPublisher

@Component
class OutboxPublishWorker(
    private val eventPublisher: EventPublisher,
    private val eventOutboxRepository: EventOutboxRepository,
    private val externalCallLogger: ExternalCallLogger,
    private val healthStateManager: RedisHealthStateManager,
) {
    private val logger = KotlinLogging.logger {}

    suspend fun runOnce(batchSize: Int): Boolean {
        if (healthStateManager.isDegraded()) {
            val recovered = healthStateManager.tryRecover()
            if (!recovered) return false
        }

        val result = healthStateManager.runSafe {
            val rows = eventOutboxRepository.findUnpublished(batchSize)
            if (rows.isEmpty()) return@runSafe

            for (row in rows) {
                try {
                    externalCallLogger.execute(
                        call = "EventPublisher.publish",
                        target = "MQ",
                        retry = 0,
                        timeout = false
                    ) {
                        eventPublisher.publish(row.topic, row.type, row.payload)
                    }
                    eventOutboxRepository.markPublished(row.id)
                } catch (e: Exception) {
                    if (healthStateManager.isFailure(e)) {
                        throw e
                    }
                    logger.errorType.warn(
                        traceId = null,
                        throwable = e,
                        "call" to "EventPublisher.publish",
                        "errorType" to (e::class.simpleName ?: "UnknownException"),
                        "message" to "outboxId=${row.id} topic=${row.topic}",
                    ) {
                        "아웃박스 이벤트 발행 중 오류가 발생했습니다"
                    }
                }
            }
        }

        return result.isSuccess
    }
}

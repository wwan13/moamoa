package server.core.infra.outbox

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import server.global.logging.errorType
import server.messaging.EventPublisher
import server.messaging.health.MessagingHealthStateManager

@Component
class OutboxPublishWorker(
    private val eventPublisher: EventPublisher,
    private val eventOutboxRepository: EventOutboxRepository,
    txManager: PlatformTransactionManager,
    private val healthStateManager: MessagingHealthStateManager,
) {
    private val logger = KotlinLogging.logger {}
    private val transactionTemplate = TransactionTemplate(txManager)

    fun runOnce(batchSize: Int): Boolean {
        if (healthStateManager.isDegraded()) {
            val recovered = healthStateManager.tryRecover()
            if (!recovered) return false
        }

        val result = healthStateManager.runSafe {
            val rows = eventOutboxRepository.findUnpublished(batchSize)
            if (rows.isEmpty()) return@runSafe

            for (row in rows) {
                try {
                    val payloadJson = row.payload
                    eventPublisher.publish(
                        channel = row.topic,
                        type = row.type,
                        payloadJson = payloadJson,
                        eventId = row.eventId,
                    )
                    transactionTemplate.execute {
                        val outbox = eventOutboxRepository.findByIdOrNull(row.id) ?: return@execute
                        if (!outbox.published) {
                            outbox.markPublished()
                        }
                    }
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

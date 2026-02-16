package server.infra.messagebroker

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class OutboxEventPublisher(
    private val eventOutboxPublishWorker: OutboxPublishWorker,
    private val outboxScope: CoroutineScope,
) {

    @Scheduled(fixedDelay = 1000L)
    fun tick() {
        outboxScope.launch {
            eventOutboxPublishWorker.runOnce(batchSize = 200)
        }
    }
}

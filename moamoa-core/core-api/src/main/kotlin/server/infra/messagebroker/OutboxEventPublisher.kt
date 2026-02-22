package server.infra.messagebroker

import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import io.github.oshai.kotlinlogging.KotlinLogging.logger as kLogger
import org.springframework.stereotype.Component

@Component
class OutboxEventPublisher(
    private val eventOutboxPublishWorker: OutboxPublishWorker,
    private val outboxScope: CoroutineScope,
) {
    private val log = kLogger {}
    private var loopJob: Job? = null

    @PostConstruct
    fun start() {
        if (loopJob?.isActive == true) return
        loopJob = outboxScope.launch {
            while (isActive) {
                val executed = runCatching {
                    eventOutboxPublishWorker.runOnce(batchSize = 200)
                }.onFailure { e ->
                    log.warn("outbox publish loop failed", e)
                }.getOrDefault(false)

                if (!executed) {
                    delay(10_000)
                    continue
                }

                delay(1_000)
            }
        }
    }

    @PreDestroy
    fun stop() {
        loopJob?.cancel()
        loopJob = null
    }
}

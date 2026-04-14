package server.core.infra.outbox

import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import io.github.oshai.kotlinlogging.KotlinLogging.logger as kLogger
import org.springframework.stereotype.Component
import kotlin.time.Duration.Companion.milliseconds

@Component
class OutboxDispatchLoop(
    private val outboxEventDispatcher: OutboxEventDispatcher,
    private val outboxScope: CoroutineScope,
) {
    private var loopJob: Job? = null

    @PostConstruct
    fun start() {
        if (loopJob?.isActive == true) return
        loopJob = outboxScope.launch {
            while (isActive) {
                val executed = outboxEventDispatcher.dispatchBatch(batchSize = 200)

                if (!executed) {
                    delay(10_000.milliseconds)
                    continue
                }

                delay(1_000.milliseconds)
            }
        }
    }

    @PreDestroy
    fun stop() {
        loopJob?.cancel()
        loopJob = null
    }
}

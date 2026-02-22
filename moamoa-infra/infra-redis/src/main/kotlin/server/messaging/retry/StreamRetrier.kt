package server.messaging.retry

import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import io.github.oshai.kotlinlogging.KotlinLogging.logger as kLogger
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import server.config.StreamRetryProperties
import java.util.concurrent.atomic.AtomicBoolean

@Component
internal class StreamRetrier(
    @param:Qualifier("schedulerScope")
    private val schedulerScope: CoroutineScope,
    private val retryProcessor: StreamRetryProcessor,
    private val properties: StreamRetryProperties,
) {
    private val log = kLogger {}
    private val running = AtomicBoolean(false)
    private var retryJob: Job? = null

    @PostConstruct
    fun start() {
        if (retryJob?.isActive == true) return
        retryJob = schedulerScope.launch {
            while (isActive) {
                runCatching {
                    runOnce()
                }.onFailure { e ->
                    log.warn(e) { "Subscription retry batch failed" }
                }
                delay(properties.intervalMs.coerceAtLeast(1L))
            }
        }
    }

    @PreDestroy
    fun stop() {
        retryJob?.cancel()
        retryJob = null
    }

    suspend fun runOnce() {
        if (!running.compareAndSet(false, true)) return

        try {
            retryProcessor.processOnce()
        } finally {
            running.set(false)
        }
    }
}

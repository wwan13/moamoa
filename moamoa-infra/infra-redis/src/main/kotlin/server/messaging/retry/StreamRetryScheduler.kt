package server.messaging.retry

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
internal class StreamRetryScheduler(
    private val schedulerScope: CoroutineScope,
    private val retrier: StreamRetrier,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "60000")
    fun tick() {
        schedulerScope.launch {
            runCatching {
                retrier.runOnce()
            }.onFailure { e ->
                log.warn("Subscription retry batch failed", e)
            }
        }
    }
}

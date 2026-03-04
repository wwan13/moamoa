package server.messaging.retry

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.jupiter.api.Test
import server.config.StreamRetryProperties

class StreamRetrierTest {

    @Test
    fun `runOnce는 retryProcessor를 한 번 호출한다`() {
        val retryProcessor = mockk<StreamRetryProcessor>()
        every { retryProcessor.processOnce() } returns true

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val retrier = StreamRetrier(
            schedulerScope = scope,
            retryProcessor = retryProcessor,
            properties = StreamRetryProperties(intervalMs = 60_000),
        )

        try {
            retrier.runOnce()

            verify(exactly = 1) { retryProcessor.processOnce() }
        } finally {
            scope.cancel()
        }
    }
}

package server.config

import jakarta.annotation.PreDestroy
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import server.global.logging.errorType

@Configuration
class CoroutineConfig {

    private val logger = KotlinLogging.logger {}

    private val singleFlightWarmupExceptionHandler =
        CoroutineExceptionHandler { _, e ->
            logger.errorType.warn(
                traceId = null,
                throwable = e,
                "call" to "singleFlightWarmup",
                "errorType" to (e::class.simpleName ?: "UnknownException"),
                "message" to (e.message ?: "singleFlightWarmup failed"),
            ) {
                "워커 실행 중 오류가 발생했습니다"
            }
        }

    private val singleFlightWarmupScope =
        CoroutineScope(SupervisorJob() + singleFlightWarmupExceptionHandler)

    @Bean
    fun singleFlightWarmupScope(): CoroutineScope = singleFlightWarmupScope

    @PreDestroy
    fun shutdown() {
        singleFlightWarmupScope.cancel()
    }

    @Bean
    fun outboxScope(): CoroutineScope = CoroutineScope(SupervisorJob())
}

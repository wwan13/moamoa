package server.config

import jakarta.annotation.PreDestroy
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import server.global.logging.warnWithTraceId

@Configuration
class CoroutineConfig {

    private val logger = KotlinLogging.logger {}

    private val singleFlightWarmupExceptionHandler =
        CoroutineExceptionHandler { _, e ->
            logger.warnWithTraceId(traceId = null, throwable = e) {
                "[WORKER] result=FAIL call=singleFlightWarmup target=CoroutineScope errorCode=${e::class.simpleName ?: "UnknownException"}"
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

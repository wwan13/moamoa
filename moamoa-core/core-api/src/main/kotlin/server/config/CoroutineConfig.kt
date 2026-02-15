package server.config

import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CoroutineConfig {

    private val log = LoggerFactory.getLogger(this::class.java)

    private val singleFlightWarmupExceptionHandler =
        CoroutineExceptionHandler { _, e ->
            log.warn("single-flight warmup coroutine failed", e)
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

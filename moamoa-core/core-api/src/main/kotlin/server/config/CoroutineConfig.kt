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

    private val cacheWarmupExceptionHandler =
        CoroutineExceptionHandler { _, e ->
            log.warn("cache warmup coroutine failed", e)
        }

    private val cacheWarmupScope =
        CoroutineScope(SupervisorJob() + cacheWarmupExceptionHandler)

    @Bean
    fun cacheWarmupScope(): CoroutineScope = cacheWarmupScope

    @PreDestroy
    fun shutdown() {
        cacheWarmupScope.cancel()
    }
}

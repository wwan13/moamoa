package server.batch.config

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class BatchCoroutineConfig {

    @Bean
    fun batchScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
}
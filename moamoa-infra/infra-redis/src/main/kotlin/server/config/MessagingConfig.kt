package server.config

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
internal class MessagingConfig {

    @Bean
    fun schedulerScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
}

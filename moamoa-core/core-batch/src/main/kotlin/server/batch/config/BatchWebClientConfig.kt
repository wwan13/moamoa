package server.batch.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class BatchWebClientConfig {

    @Bean
    fun webClient() = WebClient.builder().build()
}
package server.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class TechBlogClientConfig {

    @Bean
    @ConditionalOnMissingBean(WebClient::class)
    fun techBlogClient() = WebClient.builder().build()
}
package server.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
@ConfigurationPropertiesScan
class MailConfig {

    @Bean
    @ConditionalOnMissingBean(WebClient::class)
    fun mailWebClient() = WebClient.builder().build()
}
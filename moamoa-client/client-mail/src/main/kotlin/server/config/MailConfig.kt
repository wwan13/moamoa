package server.config

import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
@ConfigurationPropertiesScan
class MailConfig {

//    @Bean
//    fun mailWebClient() = WebClient.builder().build()
}
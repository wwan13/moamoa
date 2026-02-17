package server.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient

@Configuration
@ConfigurationPropertiesScan
internal class MailConfig {

    @Bean
    @ConditionalOnMissingBean(WebClient::class)
    fun mailWebClient(): WebClient = WebClient.builder()
        .exchangeStrategies(
            ExchangeStrategies.builder()
                .codecs {
                    it.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)
                }
                .build()
        )
        .build()
}
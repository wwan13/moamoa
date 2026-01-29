package server.config

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient

@Configuration
@ConfigurationPropertiesScan
internal class WebhookConfig {

    @Bean
    @ConditionalOnMissingBean(WebClient::class)
    fun webhookWebClient(): WebClient = WebClient.builder()
        .exchangeStrategies(
            ExchangeStrategies.builder()
                .codecs {
                    it.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)
                }
                .build()
        )
        .build()

    @Bean
    fun webhookScope(): CoroutineScope =
        CoroutineScope(Dispatchers.IO + SupervisorJob())
}
package server.techblog.http.autoconfigure

import io.netty.channel.ChannelOption
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import server.techblog.http.PythonCrawlerTechBlogSource
import server.techblog.http.TechBlogCrawlerClient
import server.techblog.http.TechBlogCrawlerProperties

@Configuration
@EnableConfigurationProperties(TechBlogCrawlerProperties::class)
internal class TechBlogCrawlerClientConfig {

    @Bean
    fun techBlogCrawlerWebClient(properties: TechBlogCrawlerProperties): WebClient {
        val httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.connectTimeout.toMillis().toInt())
            .responseTimeout(properties.responseTimeout)

        return WebClient.builder()
            .baseUrl(properties.baseUrl)
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .exchangeStrategies(
                ExchangeStrategies.builder()
                    .codecs {
                        it.defaultCodecs().maxInMemorySize(properties.maxInMemorySize)
                    }
                    .build()
            )
            .build()
    }

    @Bean
    fun techBlogCrawlerClient(
        @Qualifier("techBlogCrawlerWebClient") webClient: WebClient,
    ): TechBlogCrawlerClient = TechBlogCrawlerClient(webClient)

    @Bean
    fun pythonCrawlerTechBlogSource(
        client: TechBlogCrawlerClient,
        properties: TechBlogCrawlerProperties,
    ): PythonCrawlerTechBlogSource = PythonCrawlerTechBlogSource(client, properties)
}

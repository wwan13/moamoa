package server.config

import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.time.Duration
import java.util.concurrent.TimeUnit

@Configuration
class TechBlogClientConfig {

//    @Bean
//    fun techBlogClient(): WebClient {
//        val httpClient = HttpClient.create()
//            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3_000)
//            .responseTimeout(Duration.ofSeconds(10))
//            .doOnConnected { conn ->
//                conn.addHandlerLast(ReadTimeoutHandler(10, TimeUnit.SECONDS))
//                conn.addHandlerLast(WriteTimeoutHandler(10, TimeUnit.SECONDS))
//            }
//
//        val strategies = ExchangeStrategies.builder()
//            .codecs { it.defaultCodecs().maxInMemorySize(20 * 1024 * 1024) }
//            .build()
//
//        return WebClient.builder()
//            .clientConnector(ReactorClientHttpConnector(httpClient))
//            .exchangeStrategies(strategies)
//            .build()
//    }
}
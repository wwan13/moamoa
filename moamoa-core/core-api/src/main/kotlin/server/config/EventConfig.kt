package server.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import server.messaging.StreamDefinition
import server.messaging.StreamTopic

@Configuration
class EventConfig {

    @Bean
    fun defaultTopic() = StreamTopic("moamoa-default")

    @Bean
    fun defaultStream(
        defaultTopic: StreamTopic
    ): StreamDefinition = StreamDefinition(
        topic = defaultTopic,
        group = "default-group",
        ackWhenFail = true,
        blocking = false,
        batchSize = 10,
    )

    @Bean
    fun countProcessingStream(
        defaultTopic: StreamTopic,
    ): StreamDefinition = StreamDefinition(
        topic = defaultTopic,
        group = "count-processing-group",
        ackWhenFail = true,
        blocking = false,
        batchSize = 10,
    )

    @Bean
    fun cacheHandlingStream(
        defaultTopic: StreamTopic,
    ): StreamDefinition = StreamDefinition(
        topic = defaultTopic,
        group = "cache-handling-group",
        ackWhenFail = true,
        blocking = false,
        batchSize = 10,
    )
}
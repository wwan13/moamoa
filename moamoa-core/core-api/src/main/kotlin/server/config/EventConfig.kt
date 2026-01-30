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
    fun postCacheHandlingStream(
        defaultTopic: StreamTopic,
    ): StreamDefinition = StreamDefinition(
        topic = defaultTopic,
        group = "post-cache-handling-group",
        ackWhenFail = true,
        blocking = false,
        batchSize = 10,
    )

    @Bean
    fun techBlogCacheHandlingStream(
        defaultTopic: StreamTopic,
    ): StreamDefinition = StreamDefinition(
        topic = defaultTopic,
        group = "tech-blog-cache-handling-group",
        ackWhenFail = true,
        blocking = false,
        batchSize = 10,
    )

    @Bean
    fun monitoringStream(
        defaultTopic: StreamTopic,
    ): StreamDefinition = StreamDefinition(
        topic = defaultTopic,
        group = "monitoring-group",
        ackWhenFail = false,
        blocking = false,
        batchSize = 10,
    )
}
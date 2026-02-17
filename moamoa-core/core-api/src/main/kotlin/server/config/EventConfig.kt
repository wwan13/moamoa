package server.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import server.shared.messaging.SubscriptionDefinition
import server.shared.messaging.MessageChannel

@Configuration
class EventConfig {

    @Bean
    fun defaultTopic() = MessageChannel("moamoa-default")

    @Bean
    fun defaultStream(
        defaultTopic: MessageChannel
    ): SubscriptionDefinition = SubscriptionDefinition(
        channel = defaultTopic,
        consumerGroup = "default-group",
        ackOnFailure = true,
        processSequentially = false,
        batchSize = 10,
    )

    @Bean
    fun countProcessingStream(
        defaultTopic: MessageChannel,
    ): SubscriptionDefinition = SubscriptionDefinition(
        channel = defaultTopic,
        consumerGroup = "count-processing-group",
        ackOnFailure = true,
        processSequentially = false,
        batchSize = 10,
    )

    @Bean
    fun postCacheHandlingStream(
        defaultTopic: MessageChannel,
    ): SubscriptionDefinition = SubscriptionDefinition(
        channel = defaultTopic,
        consumerGroup = "post-cache-handling-group",
        ackOnFailure = true,
        processSequentially = false,
        batchSize = 10,
    )

    @Bean
    fun techBlogCacheHandlingStream(
        defaultTopic: MessageChannel,
    ): SubscriptionDefinition = SubscriptionDefinition(
        channel = defaultTopic,
        consumerGroup = "tech-blog-cache-handling-group",
        ackOnFailure = true,
        processSequentially = false,
        batchSize = 10,
    )

    @Bean
    fun monitoringStream(
        defaultTopic: MessageChannel,
    ): SubscriptionDefinition = SubscriptionDefinition(
        channel = defaultTopic,
        consumerGroup = "monitoring-group",
        ackOnFailure = false,
        processSequentially = false,
        batchSize = 10,
    )
}

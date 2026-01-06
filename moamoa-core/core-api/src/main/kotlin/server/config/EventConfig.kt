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
    ) = StreamDefinition(
        topic = defaultTopic,
        group = "default-group",
        ackWhenFail = true,
        blocking = false,
        batchSize = 10,
    )
}
package server.infra.event

import org.springframework.stereotype.Component
import server.messaging.StreamEventPublisher

@Component
class EventPublisher(
    private val streamEventPublisher: StreamEventPublisher
) {

    suspend fun publish(event: Any, streamKey: String = "moamoa") {
        streamEventPublisher.publish(streamKey, event)
    }
}
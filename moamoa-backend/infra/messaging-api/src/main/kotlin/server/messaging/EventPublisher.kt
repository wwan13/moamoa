package server.messaging

interface EventPublisher {
    fun publish(channel: String, type: String, payloadJson: String, eventId: String)
}

package server.messaging

interface EventPublisher {
    fun publish(channel: MessageChannel, payload: Any)

    fun publish(channel: String, type: String, payloadJson: String)
}

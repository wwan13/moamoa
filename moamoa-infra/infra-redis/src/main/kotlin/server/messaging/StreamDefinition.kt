package server.messaging

data class StreamDefinition(
    val topic: StreamTopic,
    val group: String,
    val ackWhenFail: Boolean = false,
    val blocking: Boolean = true,
    val batchSize: Int = 1,
)

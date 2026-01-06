package server.messaging

class StreamEventHandler<T : Any>(
    val stream: StreamDefinition,
    val type: String,
    val payloadClass: Class<T>,
    val handler: suspend (T) -> Unit
)
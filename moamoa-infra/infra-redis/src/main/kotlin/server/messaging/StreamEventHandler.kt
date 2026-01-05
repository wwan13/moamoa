package server.messaging

class StreamEventHandler<T : Any>(
    val streamKey: String,
    val type: String,
    val payloadClass: Class<T>,
    val handler: suspend (T) -> Unit
)
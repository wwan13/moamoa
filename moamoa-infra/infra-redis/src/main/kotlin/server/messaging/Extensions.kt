package server.messaging

inline fun <reified T : Any> handleEvent(
    stream: StreamDefinition,
    noinline handler: suspend (T) -> Unit
) = StreamEventHandler(
    stream = stream,
    type = T::class.java.simpleName,
    payloadClass = T::class.java,
    handler = handler
)
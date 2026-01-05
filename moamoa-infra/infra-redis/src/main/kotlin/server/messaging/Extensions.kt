package server.messaging

inline fun <reified T : Any> handleEvent(
    streamKey: String = "moamoa",
    noinline handler: suspend (T) -> Unit
) = StreamEventHandler(
    streamKey = streamKey,
    type = T::class.java.simpleName,
    payloadClass = T::class.java,
    handler = handler
)
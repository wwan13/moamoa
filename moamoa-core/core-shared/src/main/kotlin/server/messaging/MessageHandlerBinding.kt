package server.messaging

import server.messaging.annotation.EventStream

class MessageHandlerBinding<T : Any>(
    val stream: EventStream,
    val type: String,
    val payloadClass: Class<T>,
    val handler: (T) -> Unit
)

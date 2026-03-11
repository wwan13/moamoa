package server.messaging

import server.messaging.definition.EventStream

class MessageHandlerBinding<T : Any>(
    val stream: EventStream,
    val type: String,
    val payloadClass: Class<T>,
    val handler: (T) -> Unit
)

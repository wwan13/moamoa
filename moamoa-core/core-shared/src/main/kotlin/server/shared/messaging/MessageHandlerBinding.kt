package server.shared.messaging

class MessageHandlerBinding<T : Any>(
    val subscription: SubscriptionDefinition,
    val type: String,
    val payloadClass: Class<T>,
    val handler: suspend (T) -> Unit
)

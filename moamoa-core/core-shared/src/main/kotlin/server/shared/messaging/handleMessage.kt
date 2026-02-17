package server.shared.messaging

inline fun <reified T : Any> handleMessage(
    subscription: SubscriptionDefinition,
    noinline handler: suspend (T) -> Unit
) = MessageHandlerBinding(
    subscription = subscription,
    type = T::class.java.simpleName,
    payloadClass = T::class.java,
    handler = handler
)

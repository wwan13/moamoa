package server.messaging

inline fun <reified T : Any> handleMessage(
    subscription: SubscriptionDefinition,
    noinline handler: (T) -> Unit
) = MessageHandlerBinding(
    subscription = subscription,
    type = T::class.java.simpleName,
    payloadClass = T::class.java,
    handler = handler
)

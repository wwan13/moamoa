package server.messaging

inline operator fun <reified T : Any> AbstractEventHandler.invoke(
    subscription: SubscriptionDefinition,
    noinline handler: (T) -> Unit
): MessageHandlerBinding<T> = bind(
    subscription = subscription,
    payloadClass = T::class.java,
    handler = handler
)

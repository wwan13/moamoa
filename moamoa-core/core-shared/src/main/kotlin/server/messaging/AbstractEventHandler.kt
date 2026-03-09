package server.messaging

abstract class AbstractEventHandler(
) {
    protected abstract fun <T : Any> wrap(handler: (T) -> Unit): (T) -> Unit

    open fun <T : Any> bind(
        subscription: SubscriptionDefinition,
        payloadClass: Class<T>,
        handler: (T) -> Unit
    ): MessageHandlerBinding<T> = MessageHandlerBinding(
        subscription = subscription,
        type = payloadClass.simpleName,
        payloadClass = payloadClass,
        handler = wrap(handler),
    )
}

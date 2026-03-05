package server.messaging

val <T : Any> MessageHandlerBinding<T>.subscriptionDefinition: SubscriptionDefinition
    get() = subscription

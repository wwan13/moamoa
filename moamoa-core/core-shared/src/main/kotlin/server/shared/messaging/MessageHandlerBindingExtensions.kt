package server.shared.messaging

val <T : Any> MessageHandlerBinding<T>.subscriptionDefinition: SubscriptionDefinition
    get() = subscription

package server.messaging

import org.springframework.stereotype.Component
import server.shared.messaging.MessageHandlerBinding
import server.shared.messaging.SubscriptionDefinition

@Component
internal class StreamEventHandlers(
    private val handlers: List<MessageHandlerBinding<*>>
) {
    private data class Key(val channelKey: String, val consumerGroup: String, val type: String)

    private val handlerMap: Map<Key, MessageHandlerBinding<*>> =
        handlers.associateBy { Key(it.subscription.channel.key, it.subscription.consumerGroup, it.type) }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> find(subscription: SubscriptionDefinition, type: String): MessageHandlerBinding<T>? =
        handlerMap[Key(subscription.channel.key, subscription.consumerGroup, type)] as? MessageHandlerBinding<T>

    fun subscriptions(): List<SubscriptionDefinition> =
        handlers
            .map { it.subscription }
            .distinctBy { it.channel to it.consumerGroup }
            .toList()
}

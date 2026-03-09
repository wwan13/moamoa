package server.messaging

import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component

@Component
internal class StreamEventHandlers(
    private val handlers: List<MessageHandlerBinding<*>>,
    private val context: ApplicationContext,
    private val beanFactory: ConfigurableListableBeanFactory,
) : SmartInitializingSingleton {
    private data class Key(val channelKey: String, val consumerGroup: String, val type: String)

    @Volatile
    private var allHandlers: List<MessageHandlerBinding<*>> = handlers

    @Volatile
    private var handlerMap: Map<Key, MessageHandlerBinding<*>> = allHandlers.associateBy { toKey(it) }

    override fun afterSingletonsInstantiated() {
        val discoveredHandlers = discoverComponentHandlers()
        allHandlers = (handlers + discoveredHandlers).distinctBy(::toKey)
        handlerMap = allHandlers.associateBy(::toKey)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> find(subscription: SubscriptionDefinition, type: String): MessageHandlerBinding<T>? =
        handlerMap[Key(subscription.channel.key, subscription.consumerGroup, type)] as? MessageHandlerBinding<T>

    fun subscriptions(): List<SubscriptionDefinition> =
        allHandlers
            .map { it.subscription }
            .distinctBy { it.channel to it.consumerGroup }
            .toList()

    private fun discoverComponentHandlers(): List<MessageHandlerBinding<*>> =
        context.getBeanNamesForAnnotation(Component::class.java)
            .asSequence()
            .filterNot { it.startsWith("scopedTarget.") }
            .filter { beanFactory.isSingleton(it) }
            .mapNotNull { beanName ->
                context.getBean(beanName)
            }
            .filterNot { it === this }
            .flatMap { bean ->
                bean.javaClass.methods
                    .asSequence()
                    .filter { method ->
                        method.parameterCount == 0 &&
                            method.getAnnotation(Bean::class.java) == null &&
                            MessageHandlerBinding::class.java.isAssignableFrom(method.returnType)
                    }
                    .mapNotNull { method -> method.invoke(bean) as? MessageHandlerBinding<*> }
            }
            .toList()

    private fun toKey(handler: MessageHandlerBinding<*>): Key =
        Key(handler.subscription.channel.key, handler.subscription.consumerGroup, handler.type)
}

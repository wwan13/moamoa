package server.messaging

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.aop.support.AopUtils
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.context.ApplicationContext
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.util.ClassUtils
import org.springframework.util.ReflectionUtils
import server.messaging.annotation.EventHandler
import server.messaging.definition.EventStream

@Component
internal class StreamEventHandlers(
    private val context: ApplicationContext,
    private val beanFactory: ConfigurableListableBeanFactory,
    txManager: PlatformTransactionManager?,
) {
    private data class Key(val channelKey: String, val consumerGroup: String, val type: String)

    private val logger = KotlinLogging.logger {}
    private val transactionTemplate: TransactionTemplate? = txManager?.let { TransactionTemplate(it) }

    private val allHandlers: List<StreamMessageHandler> = discoverAnnotatedHandlers().distinctBy(::toKey)
    private val handlerMap: Map<Key, StreamMessageHandler> = allHandlers.associateBy(::toKey)

    fun find(stream: EventStream, type: String): StreamMessageHandler? =
        handlerMap[Key(stream.channel.key, stream.consumerGroup, type)]

    fun streams(): List<EventStream> =
        allHandlers
            .map { it.stream }
            .distinctBy { it.channel.key to it.consumerGroup }
            .toList()

    private fun discoverAnnotatedHandlers(): List<StreamMessageHandler> =
        beanFactory.beanDefinitionNames
            .asSequence()
            .filterNot { it.startsWith("scopedTarget.") }
            .filter { beanFactory.isSingleton(it) }
            .mapNotNull { beanName ->
                val rawClass = context.getType(beanName) ?: return@mapNotNull null
                beanName to ClassUtils.getUserClass(rawClass)
            }
            .filterNot { (_, targetClass) -> targetClass == StreamEventHandlers::class.java }
            .flatMap { (beanName, targetClass) -> resolveAnnotatedHandlers(beanName, targetClass).asSequence() }
            .toList()

    private fun resolveAnnotatedHandlers(beanName: String, targetClass: Class<*>): List<StreamMessageHandler> {
        return targetClass.methods
            .asSequence()
            .mapNotNull { method ->
                val eventHandler = AnnotatedElementUtils.findMergedAnnotation(method, EventHandler::class.java)
                if (method.parameterCount != 1) {
                    if (eventHandler != null) {
                        logger.warn {
                            "Skip event handler method. exactly one parameter is required: ${targetClass.name}.${method.name}"
                        }
                    }
                    return@mapNotNull null
                }

                eventHandler ?: return@mapNotNull null

                toHandler(
                    beanName = beanName,
                    method = method,
                    targetClass = targetClass,
                    methodName = method.name,
                    paramType = method.parameterTypes[0],
                    payloadClass = eventHandler.value.java,
                    stream = eventHandler.stream,
                    transactional = eventHandler.transaction,
                )
            }
            .toList()
    }

    private fun toHandler(
        beanName: String,
        method: java.lang.reflect.Method,
        targetClass: Class<*>,
        methodName: String,
        paramType: Class<*>,
        payloadClass: Class<out Any>,
        stream: EventStream,
        transactional: Boolean,
    ): StreamMessageHandler? {
        if (!paramType.isAssignableFrom(payloadClass)) {
            logger.warn {
                "Skip event handler method. parameter type mismatch: ${targetClass.name}.$methodName " +
                    "param=${paramType.name} event=${payloadClass.name}"
            }
            return null
        }

        val handler: (Any) -> Unit = { payload ->
            val bean = context.getBean(beanName)

            if (!transactional) {
                invokeMethod(bean, method, payload)
            } else {
                val tx = requireNotNull(transactionTemplate) {
                    "No PlatformTransactionManager for @TransactionEventHandler: ${targetClass.name}.$methodName"
                }
                tx.executeWithoutResult { invokeMethod(bean, method, payload) }
            }
        }

        return StreamMessageHandler(
            stream = stream,
            type = payloadClass.simpleName,
            payloadClass = payloadClass,
            handler = handler,
        )
    }

    private fun toKey(handler: StreamMessageHandler): Key =
        Key(handler.stream.channel.key, handler.stream.consumerGroup, handler.type)

    private fun invokeMethod(bean: Any, method: java.lang.reflect.Method, payload: Any) {
        val invocable = AopUtils.selectInvocableMethod(method, bean.javaClass)
        ReflectionUtils.makeAccessible(invocable)
        invocable.invoke(bean, payload)
    }
}

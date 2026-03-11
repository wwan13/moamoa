package server.messaging

import server.messaging.definition.EventStream

abstract class AbstractEventHandler{

    abstract fun <T : Any> wrap(handler: (T) -> Unit): (T) -> Unit

    inline operator fun <reified T : Any> invoke(
        stream: EventStream,
        noinline handler: (T) -> Unit
    ): MessageHandlerBinding<T> {
        val payloadClass = T::class.java
        return MessageHandlerBinding(
            stream = stream,
            type = payloadClass.simpleName,
            payloadClass = payloadClass,
            handler = wrap(handler),
        )
    }
}

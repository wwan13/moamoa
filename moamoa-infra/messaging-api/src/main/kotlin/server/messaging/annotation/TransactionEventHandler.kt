package server.messaging.annotation

import org.springframework.core.annotation.AliasFor
import server.messaging.definition.EventStream
import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@EventHandler(
    value = Any::class,
    stream = EventStream.MONITORING,
    transaction = true,
)
annotation class TransactionEventHandler(
    @get:AliasFor(annotation = EventHandler::class, attribute = "value")
    val value: KClass<out Any>,

    @get:AliasFor(annotation = EventHandler::class, attribute = "stream")
    val stream: EventStream,
)

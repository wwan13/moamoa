package server.messaging.annotation

import org.springframework.core.annotation.AliasFor
import server.messaging.definition.EventStream

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@EventHandler(
    stream = EventStream.MONITORING,
    transaction = true,
)
annotation class TransactionEventHandler(
    @get:AliasFor(annotation = EventHandler::class, attribute = "stream")
    val stream: EventStream,
)

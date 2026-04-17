package server.messaging.annotation

import server.messaging.definition.EventStream

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class EventHandler(
    val stream: EventStream,
    val transaction: Boolean = false,
)

package server.messaging.annotation

import server.messaging.definition.EventStream
import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class EventHandler(
    val value: KClass<out Any>,
    val stream: EventStream,
    val transaction: Boolean = false,
)

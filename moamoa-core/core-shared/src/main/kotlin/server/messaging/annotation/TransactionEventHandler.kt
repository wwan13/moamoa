package server.messaging.annotation

import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class TransactionEventHandler(
    val value: KClass<out Any>,
    val stream: EventStream,
)

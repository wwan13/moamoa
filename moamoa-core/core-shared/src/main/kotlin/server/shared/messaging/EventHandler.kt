package server.shared.messaging

import org.springframework.context.annotation.Bean

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Bean
annotation class EventHandler

package server.infra.event

import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.reactive.TransactionalEventPublisher

@Component
class EventPublisher(
    applicationEventPublisher: ApplicationEventPublisher
) {

    private val transactionalEventPublisher = TransactionalEventPublisher(applicationEventPublisher)

    suspend fun publish(event: Any) {
        transactionalEventPublisher.publishEvent(event).awaitSingleOrNull()
    }
}
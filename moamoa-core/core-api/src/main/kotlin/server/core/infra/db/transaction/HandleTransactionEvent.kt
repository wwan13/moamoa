package server.core.infra.db.transaction

import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import server.messaging.MessageHandlerBinding
import server.messaging.SubscriptionDefinition

@Component
class HandleTransactionEvent(
    txManager: PlatformTransactionManager,
) {
    private val transactionTemplate = TransactionTemplate(txManager)

    operator fun <T : Any> invoke(
        subscription: SubscriptionDefinition,
        payloadClass: Class<T>,
        handler: (T) -> Unit,
    ): MessageHandlerBinding<T> = MessageHandlerBinding(
        subscription = subscription,
        type = payloadClass.simpleName,
        payloadClass = payloadClass,
    ) { event ->
        transactionTemplate.execute {
            handler(event)
        }
    }

    operator fun invoke(
        handler: () -> Unit,
    ) {
        transactionTemplate.execute {
            handler()
        }
    }
}

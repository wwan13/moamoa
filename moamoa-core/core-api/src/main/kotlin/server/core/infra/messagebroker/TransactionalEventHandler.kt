package server.core.infra.messagebroker

import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import server.messaging.AbstractEventHandler

@Component
class TransactionalEventHandler(
    txManager: PlatformTransactionManager,
) : AbstractEventHandler() {
    private val transactionTemplate = TransactionTemplate(txManager)

    override fun <T : Any> wrap(handler: (T) -> Unit): (T) -> Unit = { event ->
        transactionTemplate.executeWithoutResult {
            handler(event)
        }
    }
}

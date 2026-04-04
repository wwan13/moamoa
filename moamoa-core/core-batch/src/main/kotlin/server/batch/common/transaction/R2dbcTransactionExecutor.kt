package server.batch.common.transaction

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.springframework.stereotype.Component
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.ReactiveTransactionManager
import org.springframework.transaction.support.DefaultTransactionDefinition
import org.springframework.transaction.reactive.TransactionalOperator

@Component
internal class R2dbcTransactionExecutor(
    txManager: ReactiveTransactionManager
) {
    private val txOperator = TransactionalOperator.create(
        txManager,
        DefaultTransactionDefinition().apply {
            propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
        }
    )

    suspend fun <T> execute(action: suspend () -> T): T =
        txOperator.transactional(mono { action() }).awaitSingle()
}

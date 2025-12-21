package server.admin.infra.db

import io.r2dbc.spi.ConnectionFactory
import org.springframework.r2dbc.connection.R2dbcTransactionManager
import org.springframework.stereotype.Component
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait
import org.springframework.transaction.support.DefaultTransactionDefinition
import server.admin.application.AdminTransactional
import java.util.concurrent.ConcurrentHashMap

@Component
class AdminR2dbcTransactional(
    connectionFactory: ConnectionFactory
) : AdminTransactional {

    private val manager = R2dbcTransactionManager(connectionFactory)
    private val operators = ConcurrentHashMap<Propagation, TransactionalOperator>()

    override suspend fun <T> invoke(
        propagation: Propagation,
        block: suspend () -> T
    ): T {
        val operator = operators.computeIfAbsent(propagation) { p ->
            val def = DefaultTransactionDefinition().apply {
                propagationBehavior = p.toTxPropagationBehavior()
            }
            TransactionalOperator.create(manager, def)
        }
        return operator.executeAndAwait { block() }
    }

    private fun Propagation.toTxPropagationBehavior(): Int = when (this) {
        Propagation.REQUIRED -> TransactionDefinition.PROPAGATION_REQUIRED
        Propagation.REQUIRES_NEW -> TransactionDefinition.PROPAGATION_REQUIRES_NEW
        Propagation.SUPPORTS -> TransactionDefinition.PROPAGATION_SUPPORTS
        Propagation.NOT_SUPPORTED -> TransactionDefinition.PROPAGATION_NOT_SUPPORTED
        Propagation.MANDATORY -> TransactionDefinition.PROPAGATION_MANDATORY
        Propagation.NEVER -> TransactionDefinition.PROPAGATION_NEVER
        Propagation.NESTED -> TransactionDefinition.PROPAGATION_NESTED
    }
}
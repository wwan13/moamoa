package server.infra.db

import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.support.TransactionTemplate
import server.application.Transactional

@Component
class JpaTransactional(
    private val transactionManager: PlatformTransactionManager
) : Transactional {

    override fun <T> invoke(
        readOnly: Boolean,
        propagation: Propagation,
        block: () -> T
    ): T {
        val template = TransactionTemplate(transactionManager).apply {
            isReadOnly = readOnly
            propagationBehavior = when (propagation) {
                Propagation.REQUIRED -> TransactionDefinition.PROPAGATION_REQUIRED
                Propagation.REQUIRES_NEW -> TransactionDefinition.PROPAGATION_REQUIRES_NEW
                Propagation.SUPPORTS -> TransactionDefinition.PROPAGATION_SUPPORTS
                Propagation.MANDATORY -> TransactionDefinition.PROPAGATION_MANDATORY
                Propagation.NOT_SUPPORTED -> TransactionDefinition.PROPAGATION_NOT_SUPPORTED
                Propagation.NEVER -> TransactionDefinition.PROPAGATION_NEVER
                Propagation.NESTED -> TransactionDefinition.PROPAGATION_NESTED
            }
        }

        return template.execute { block() }!!
    }
}
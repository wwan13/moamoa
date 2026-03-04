package server.infra.db.transaction

import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.support.DefaultTransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.ConcurrentHashMap

@Component
class Transactional(
    private val txManager: PlatformTransactionManager,
    private val transactionScope: TransactionScope,
) {

    private val templates = ConcurrentHashMap<Propagation, TransactionTemplate>()

    operator fun <T> invoke(
        propagation: Propagation = Propagation.REQUIRED,
        block: TransactionScope.() -> T,
    ): T {
        val template = templates.computeIfAbsent(propagation) { p ->
            val def = DefaultTransactionDefinition().apply {
                propagationBehavior = p.toTxPropagationBehavior()
            }
            TransactionTemplate(txManager, def)
        }

        return template.execute { _: TransactionStatus -> transactionScope.block() }
            ?: throw IllegalStateException("Transactional returned null")
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

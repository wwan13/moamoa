package server.batch.common.transaction

import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

@Component
internal class AfterCommitExecutor {

    fun execute(action: suspend () -> Unit) {
        TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {
                override fun afterCommit() {
                    runBlocking {
                        action()
                    }
                }
            }
        )
    }
}
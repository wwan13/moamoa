package server.admin.application

import org.springframework.transaction.annotation.Propagation

interface AdminTransactional {
    suspend operator fun <T> invoke(
        propagation: Propagation = Propagation.REQUIRED,
        block: suspend () -> T
    ): T
}
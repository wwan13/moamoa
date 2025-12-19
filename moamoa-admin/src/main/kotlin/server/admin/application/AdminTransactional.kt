package server.admin.application

import org.springframework.transaction.annotation.Propagation

interface AdminTransactional {
    operator fun <T> invoke(
        readOnly: Boolean = false,
        propagation: Propagation = Propagation.REQUIRED,
        block: () -> T
    ): T
}
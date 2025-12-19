package server.application

import org.springframework.transaction.annotation.Propagation

interface Transactional {
    operator fun <T> invoke(
        readOnly: Boolean = false,
        propagation: Propagation = Propagation.REQUIRED,
        block: () -> T
    ): T
}
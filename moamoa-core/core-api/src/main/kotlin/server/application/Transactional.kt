package server.application

import org.springframework.transaction.annotation.Propagation

interface Transactional {
    suspend operator fun <T> invoke(
        propagation: Propagation = Propagation.REQUIRED,
        block: suspend () -> T
    ): T
}
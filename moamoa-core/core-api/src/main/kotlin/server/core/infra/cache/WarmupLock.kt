package server.core.infra.cache

import org.springframework.stereotype.Component
import server.lock.KeyedLock

@Component
class WarmupLock(
    private val keyedLock: KeyedLock,
) {
    fun withLock(
        key: String,
        block: () -> Unit,
    ) {
        keyedLock.withLock(key) { block() }
    }
}

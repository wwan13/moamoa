package server.global.lock

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@Component
class KeyedMutex {

    private data class Entry(
        val mutex: Mutex,
        val refCount: AtomicInteger
    )

    private val map = ConcurrentHashMap<String, Entry>()

    suspend fun <T> withLock(key: String, block: suspend () -> T): T {
        val entry = map.compute(key) { _, old ->
            val e = old ?: Entry(Mutex(), AtomicInteger(0))
            e.refCount.incrementAndGet()
            e
        } ?: throw IllegalStateException("Unable to acquire lock for $key")

        try {
            return entry.mutex.withLock { block() }
        } finally {
            if (entry.refCount.decrementAndGet() == 0) {
                map.remove(key, entry)
            }
        }
    }
}
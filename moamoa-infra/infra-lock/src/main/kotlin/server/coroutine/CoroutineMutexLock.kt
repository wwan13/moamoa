package server.coroutine

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.springframework.stereotype.Component
import server.shared.lock.KeyedLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@Component("coroutineMutexLock")
internal class CoroutineMutexLock : KeyedLock {

    private data class Entry(
        val mutex: Mutex,
        val refCount: AtomicInteger,
    )

    private val locksByKey = ConcurrentHashMap<String, Entry>()

    override suspend fun <T> withLock(key: String, block: suspend () -> T): T {
        val entry = locksByKey.compute(key) { _, old ->
            val current = old ?: Entry(Mutex(), AtomicInteger(0))
            current.refCount.incrementAndGet()
            current
        } ?: throw IllegalStateException("Unable to acquire lock for $key")

        try {
            return entry.mutex.withLock { block() }
        } finally {
            if (entry.refCount.decrementAndGet() == 0) {
                locksByKey.remove(key, entry)
            }
        }
    }
}

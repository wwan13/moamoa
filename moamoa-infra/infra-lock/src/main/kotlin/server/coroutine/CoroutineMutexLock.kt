package server.coroutine

import org.springframework.stereotype.Component
import server.shared.lock.KeyedLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock

@Component("coroutineMutexLock")
internal class CoroutineMutexLock : KeyedLock {

    private data class Entry(
        val lock: ReentrantLock,
        val refCount: AtomicInteger,
    )

    private val locksByKey = ConcurrentHashMap<String, Entry>()

    override fun <T> withLock(key: String, block: () -> T): T {
        val entry = locksByKey.compute(key) { _, old ->
            val current = old ?: Entry(ReentrantLock(), AtomicInteger(0))
            current.refCount.incrementAndGet()
            current
        } ?: throw IllegalStateException("Unable to acquire lock for $key")

        entry.lock.lock()
        try {
            return block()
        } finally {
            entry.lock.unlock()
            if (entry.refCount.decrementAndGet() == 0) {
                locksByKey.remove(key, entry)
            }
        }
    }
}

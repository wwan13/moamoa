package server.infra.cache

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import server.global.lock.KeyedMutex
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

@Component
class WarmupCoordinator(
    private val keyedMutex: KeyedMutex,
    @param:Qualifier("singleFlightWarmupScope")
    private val warmupScope: CoroutineScope,
) {
    private val inFlightKeys = ConcurrentHashMap.newKeySet<String>()

    fun launchIfAbsent(key: String, block: suspend () -> Unit) {
        if (!inFlightKeys.add(key)) return

        warmupScope.launch {
            try {
                keyedMutex.withLock(key) {
                    block()
                }
            } finally {
                inFlightKeys.remove(key)
            }
        }
    }

    companion object {
        fun msetKey(namespace: String, keys: Collection<String>): String {
            val csv = keys.asSequence().distinct().sorted().joinToString(",")
            return "warmup:$namespace:mset:${sha256(csv)}"
        }

        private fun sha256(value: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
            return digest.joinToString("") { byte -> "%02x".format(byte) }
        }
    }
}

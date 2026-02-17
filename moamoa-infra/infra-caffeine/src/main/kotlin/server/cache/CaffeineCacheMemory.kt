package server.cache

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.stereotype.Component
import server.shared.cache.CacheMemory
import java.util.concurrent.ConcurrentHashMap

@Component("caffeineCacheMemory")
internal class CaffeineCacheMemory(
    private val objectMapper: ObjectMapper
) : CacheMemory {
    private val cache = Caffeine.newBuilder().build<String, CacheEntry>()
    private val keyLocks = ConcurrentHashMap<String, Any>()

    override suspend fun <T> get(key: String, type: Class<T>): T? {
        val json = getValue(key) ?: return null
        return runCatching { objectMapper.readValue(json, type) }.getOrNull()
    }

    override suspend fun <T> get(key: String, typeRef: TypeReference<T>): T? {
        val json = getValue(key) ?: return null
        return runCatching { objectMapper.readValue(json, typeRef) }.getOrNull()
    }

    override suspend fun <T> set(key: String, value: T, ttlMillis: Long?) {
        cache.put(key, CacheEntry(objectMapper.writeValueAsString(value), toExpireAt(ttlMillis)))
    }

    override suspend fun <T> setIfAbsent(key: String, value: T, ttlMillis: Long?): Boolean {
        val lock = keyLocks.computeIfAbsent(key) { Any() }
        synchronized(lock) {
            val existing = cache.getIfPresent(key)
            if (existing != null && !existing.isExpired()) {
                return false
            }

            cache.put(key, CacheEntry(objectMapper.writeValueAsString(value), toExpireAt(ttlMillis)))
            return true
        }
    }

    override suspend fun incr(key: String): Long {
        val lock = keyLocks.computeIfAbsent(key) { Any() }
        synchronized(lock) {
            val current = getValue(key)?.toLongOrNull() ?: 0L
            val next = current + 1
            cache.put(key, CacheEntry(next.toString(), null))
            return next
        }
    }

    override suspend fun decrBy(key: String, delta: Long): Long {
        val lock = keyLocks.computeIfAbsent(key) { Any() }
        synchronized(lock) {
            val current = getValue(key)?.toLongOrNull() ?: 0L
            val next = current - delta
            cache.put(key, CacheEntry(next.toString(), null))
            return next
        }
    }

    override suspend fun evict(key: String) {
        cache.invalidate(key)
    }

    override suspend fun evictByPrefix(prefix: String) {
        cache.asMap().keys
            .filter { it.startsWith(prefix) }
            .forEach(cache::invalidate)
    }

    override suspend fun mget(keys: Collection<String>): Map<String, String?> {
        return keys.associateWith(::getValue)
    }

    override suspend fun <T> mgetAs(keys: Collection<String>, typeRef: TypeReference<T>): Map<String, T?> {
        return keys.associateWith { key ->
            getValue(key)?.let { json ->
                runCatching { objectMapper.readValue(json, typeRef) }.getOrNull()
            }
        }
    }

    override suspend fun mset(valuesByKey: Map<String, Any>, ttlMillis: Long?) {
        val expireAt = toExpireAt(ttlMillis)
        valuesByKey.forEach { (key, value) ->
            cache.put(key, CacheEntry(objectMapper.writeValueAsString(value), expireAt))
        }
    }

    private fun getValue(key: String): String? {
        val entry = cache.getIfPresent(key) ?: return null
        if (entry.isExpired()) {
            cache.invalidate(key)
            return null
        }

        return entry.value
    }

    private fun toExpireAt(ttlMillis: Long?): Long? {
        return ttlMillis?.let { System.currentTimeMillis() + it }
    }

    private data class CacheEntry(
        val value: String,
        val expireAtMillis: Long?
    ) {
        fun isExpired(nowMillis: Long = System.currentTimeMillis()): Boolean {
            return expireAtMillis != null && nowMillis >= expireAtMillis
        }
    }
}

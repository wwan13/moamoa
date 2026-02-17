package server.shared.cache

import com.fasterxml.jackson.core.type.TypeReference

interface CacheMemory {
    suspend fun <T> get(key: String, type: Class<T>): T?

    suspend fun <T> get(key: String, typeRef: TypeReference<T>): T?

    suspend fun <T> set(key: String, value: T, ttlMillis: Long?)

    suspend fun <T> setIfAbsent(key: String, value: T, ttlMillis: Long?): Boolean

    suspend fun incr(key: String): Long

    suspend fun decrBy(key: String, delta: Long): Long

    suspend fun evict(key: String)

    suspend fun evictByPrefix(prefix: String)

    suspend fun mget(keys: Collection<String>): Map<String, String?>

    suspend fun <T> mgetAs(keys: Collection<String>, typeRef: TypeReference<T>): Map<String, T?>

    suspend fun mset(valuesByKey: Map<String, Any>, ttlMillis: Long? = null)
}

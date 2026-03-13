package server.cache

import com.fasterxml.jackson.core.type.TypeReference

interface CacheMemory {
    fun <T> get(key: String, type: Class<T>): T?

    fun <T> get(key: String, typeRef: TypeReference<T>): T?

    fun <T> set(key: String, value: T, ttlMillis: Long?)

    fun <T> setIfAbsent(key: String, value: T, ttlMillis: Long?): Boolean

    fun incr(key: String): Long

    fun decrBy(key: String, delta: Long): Long

    fun evict(key: String)

    fun evictByPrefix(prefix: String)

    fun mget(keys: Collection<String>): Map<String, String?>

    fun <T> mgetAs(keys: Collection<String>, typeRef: TypeReference<T>): Map<String, T?>

    fun mset(valuesByKey: Map<String, Any>, ttlMillis: Long? = null)
}

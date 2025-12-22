package server.infra.cache

interface CacheMemory {
    suspend fun <T> get(key: String, type: Class<T>): T?

    suspend fun <T> set(key: String, value: T, ttlMillis: Long? = null)

    suspend fun <T> setIfAbsent(key: String, value: T, ttlMillis: Long? = null): Boolean

    suspend fun evict(key: String)
}
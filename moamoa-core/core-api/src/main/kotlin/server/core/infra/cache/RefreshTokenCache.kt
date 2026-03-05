package server.core.infra.cache

import org.springframework.stereotype.Component
import server.cache.CacheMemory
import server.cache.get

@Component
class RefreshTokenCache(
    private val cacheMemory: server.cache.CacheMemory
) {

    private val refreshTokenPrefix = "REFRESH_TOKEN:"

    private fun key(memberId: Long): String =
        refreshTokenPrefix + memberId

    fun set(
        memberId: Long,
        refreshToken: String,
        ttlMillis: Long,
    ) {
        cacheMemory.set(
            key = key(memberId),
            value = refreshToken,
            ttlMillis = ttlMillis
        )
    }

    fun get(memberId: Long): String? {
        return cacheMemory.get(key(memberId))
    }

    fun evict(memberId: Long) {
        cacheMemory.evict(key(memberId))
    }
}

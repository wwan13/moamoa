package server.application

import org.springframework.stereotype.Component
import server.infra.cache.CacheMemory
import server.infra.cache.get

@Component
class RefreshTokenCache(
    private val cacheMemory: CacheMemory,
) {
    private val refreshTokenPrefix = "REFRESH_TOKEN:"

    private fun key(memberId: Long): String =
        refreshTokenPrefix + memberId

    suspend fun set(
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

    suspend fun get(memberId: Long): String? {
        return cacheMemory.get<String>(key(memberId))
    }

    suspend fun evict(memberId: Long) {
        cacheMemory.evict(key(memberId))
    }
}
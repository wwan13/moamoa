package server.infra.cache

import org.springframework.stereotype.Component
import server.application.cache.RefreshTokenCache

@Component
class RedisRefreshTokenCache(
    private val cacheMemory: CacheMemory
) : RefreshTokenCache {

    private val refreshTokenPrefix = "REFRESH_TOKEN:"

    private fun key(memberId: Long): String =
        refreshTokenPrefix + memberId

    override suspend fun set(
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

    override suspend fun get(memberId: Long): String? {
        return cacheMemory.get(key(memberId))
    }

    override suspend fun evict(memberId: Long) {
        cacheMemory.evict(key(memberId))
    }
}
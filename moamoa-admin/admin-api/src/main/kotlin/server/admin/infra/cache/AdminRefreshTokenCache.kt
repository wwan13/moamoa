package server.admin.infra.cache

import org.springframework.stereotype.Component
import server.shared.cache.CacheMemory
import server.shared.cache.get

@Component
class AdminRefreshTokenCache(
    private val cacheMemory: CacheMemory
) {

    private val refreshTokenPrefix = "REFRESH_TOKEN:ADMIN:"

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

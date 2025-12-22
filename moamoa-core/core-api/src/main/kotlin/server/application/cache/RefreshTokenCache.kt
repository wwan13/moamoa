package server.application.cache

interface RefreshTokenCache {

    suspend fun set(
        memberId: Long,
        refreshToken: String,
        ttlMillis: Long,
    )

    suspend fun get(memberId: Long): String?

    suspend fun evict(memberId: Long)
}
package server.core.infra.cache

import org.springframework.stereotype.Component
import server.cache.CacheMemory
import server.cache.get

@Component
class SocialMemberSessionCache(
    private val cacheMemory: server.cache.CacheMemory
) {

    private val prefix = "SOCIAL:SIGNUP:"
    private val oneMinute = 60_000L

    fun set(token: String, memberId: Long) {
        cacheMemory.set(
            key = key(token),
            value = memberId,
            ttlMillis = oneMinute
        )
    }

    fun get(token: String): Long? {
        return cacheMemory.get(key(token))
    }

    fun evict(token: String) {
        return cacheMemory.evict(key(token))
    }

    private fun key(token: String): String =
        "$prefix$token"
}

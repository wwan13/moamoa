package server.set

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import server.set.SetMemory

@Component
internal class RedisSetMemory(
    @param:Qualifier("cacheStringRedisTemplate")
    private val redis: StringRedisTemplate,
) : SetMemory {
    private val ops = redis.opsForSet()

    override fun add(key: String, value: String): Boolean = (ops.add(key, value) ?: 0L) > 0L

    override fun members(key: String): Set<String> = ops.members(key) ?: emptySet()

    override fun remove(key: String, value: String): Long = ops.remove(key, value) ?: 0L
}
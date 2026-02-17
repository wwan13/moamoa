package server.set

import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Component
import server.shared.set.SetMemory

@Component
internal class RedisSetMemory(
    private val redis: ReactiveRedisTemplate<String, String>,
) : SetMemory {
    private val ops = redis.opsForSet()

    override suspend fun add(key: String, value: String): Boolean =
        (ops.add(key, value).awaitSingle()) > 0L

    override suspend fun members(key: String): Set<String> =
        ops.members(key).collectList().awaitSingle().toSet()

    override suspend fun remove(key: String, value: String): Long =
        ops.remove(key, value).awaitSingle()
}

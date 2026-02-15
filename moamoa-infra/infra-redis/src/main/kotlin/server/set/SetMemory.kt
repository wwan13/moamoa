package server.set

import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Component

@Component
class SetMemory(
    private val redis: ReactiveRedisTemplate<String, String>,
) {
    private val ops = redis.opsForSet()

    suspend fun add(key: String, value: String): Boolean =
        (ops.add(key, value).awaitSingle()) > 0L

    suspend fun members(key: String): Set<String> =
        ops.members(key).collectList().awaitSingle().toSet()

    suspend fun remove(key: String, value: String): Long =
        ops.remove(key, value).awaitSingle()
}

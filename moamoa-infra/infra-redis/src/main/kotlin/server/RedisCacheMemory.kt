package server

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Component
import server.infra.cache.CacheMemory
import java.time.Duration

@Component
class RedisCacheMemory(
    private val reactiveRedisTemplate: ReactiveRedisTemplate<String, String>,
    private val objectMapper: ObjectMapper
) : CacheMemory {

    private val valueOps get() = reactiveRedisTemplate.opsForValue()

    override suspend fun <T> get(key: String, type: Class<T>): T? {
        val json = valueOps.get(key).awaitFirstOrNull() ?: return null
        return runCatching { objectMapper.readValue(json, type) }.getOrNull()
    }

    override suspend fun <T> set(
        key: String,
        value: T,
        ttlMillis: Long?
    ) {
        val json = objectMapper.writeValueAsString(value)

        if (ttlMillis == null) {
            valueOps.set(key, json).awaitSingle()
        } else {
            valueOps.set(key, json, Duration.ofMillis(ttlMillis)).awaitSingle()
        }
    }

    override suspend fun <T> setIfAbsent(
        key: String,
        value: T,
        ttlMillis: Long?
    ): Boolean {
        val json = objectMapper.writeValueAsString(value)

        return if (ttlMillis == null) {
            valueOps.setIfAbsent(key, json).awaitSingle() ?: false
        } else {
            valueOps.setIfAbsent(key, json, Duration.ofMillis(ttlMillis)).awaitSingle() ?: false
        }
    }

    override suspend fun evict(key: String) {
        reactiveRedisTemplate.delete(key).awaitSingle()
    }
}
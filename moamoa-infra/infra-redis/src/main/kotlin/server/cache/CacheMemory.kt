package server.cache

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class CacheMemory(
    private val reactiveRedisTemplate: ReactiveRedisTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {

    private val valueOps get() = reactiveRedisTemplate.opsForValue()

    final suspend inline fun <reified T> get(key: String): T? {
        return get(key, T::class.java)
    }

    suspend fun <T> get(key: String, type: Class<T>): T? {
        val json = valueOps.get(key).awaitFirstOrNull() ?: return null
        return runCatching { objectMapper.readValue(json, type) }.getOrNull()
    }

    suspend fun <T> set(
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

    suspend fun <T> setIfAbsent(
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

    suspend fun incr(key: String): Long {
        return valueOps.increment(key).awaitSingle()
    }

    suspend fun evict(key: String) {
        reactiveRedisTemplate.delete(key).awaitSingle()
    }
}
package server.cache

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.ScanOptions
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class CacheMemory(
    private val reactiveRedisTemplate: ReactiveRedisTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {

    private val valueOps get() = reactiveRedisTemplate.opsForValue()

    final suspend inline fun <reified T> get(key: String): T? =
        get(key, object : TypeReference<T>() {})

    suspend fun <T> get(key: String, type: Class<T>): T? {
        val json = valueOps.get(key).awaitFirstOrNull() ?: return null
        return runCatching { objectMapper.readValue(json, type) }.getOrNull()
    }

    suspend fun <T> get(key: String, typeRef: TypeReference<T>): T? {
        val json = valueOps.get(key).awaitFirstOrNull() ?: return null
        return runCatching { objectMapper.readValue(json, typeRef) }.getOrNull()
    }

    suspend fun <T> set(key: String, value: T, ttlMillis: Long?) {
        val json = objectMapper.writeValueAsString(value)
        if (ttlMillis == null) valueOps.set(key, json).awaitSingle()
        else valueOps.set(key, json, Duration.ofMillis(ttlMillis)).awaitSingle()
    }

    suspend fun <T> setIfAbsent(key: String, value: T, ttlMillis: Long?): Boolean {
        val json = objectMapper.writeValueAsString(value)
        return if (ttlMillis == null) {
            valueOps.setIfAbsent(key, json).awaitSingle() ?: false
        } else {
            valueOps.setIfAbsent(key, json, Duration.ofMillis(ttlMillis)).awaitSingle() ?: false
        }
    }

    suspend fun incr(key: String): Long =
        valueOps.increment(key).awaitSingle()

    suspend fun evict(key: String) {
        reactiveRedisTemplate.delete(key).awaitSingle()
    }

    suspend fun evictByPrefix(prefix: String) {
        val pattern = "$prefix*"

        val scanOptions = ScanOptions.scanOptions()
            .match(pattern)
            .count(500)
            .build()

        reactiveRedisTemplate
            .scan(scanOptions)
            .asFlow()
            .collect { key ->
                reactiveRedisTemplate.delete(key).awaitSingle()
            }
    }

    suspend fun mget(keys: Collection<String>): Map<String, String?> {
        if (keys.isEmpty()) return emptyMap()
        val keyList = keys.toList()
        val values = valueOps.multiGet(keyList).awaitSingle() ?: emptyList()
        return keyList.mapIndexed { idx, key -> key to values.getOrNull(idx) }.toMap()
    }

    final suspend inline fun <reified T> mgetAs(keys: Collection<String>): Map<String, T?> =
        mgetAs(keys, object : TypeReference<T>() {})

    suspend fun <T> mgetAs(keys: Collection<String>, typeRef: TypeReference<T>): Map<String, T?> {
        val raw = mget(keys)
        if (raw.isEmpty()) return emptyMap()

        return raw.mapValues { (_, json) ->
            if (json == null) null
            else runCatching { objectMapper.readValue(json, typeRef) }.getOrNull()
        }
    }

    suspend fun mset(valuesByKey: Map<String, Any>, ttlMillis: Long? = null) {
        if (valuesByKey.isEmpty()) return

        val jsonByKey = valuesByKey.mapValues { (_, v) -> objectMapper.writeValueAsString(v) }
        valueOps.multiSet(jsonByKey).awaitSingle()

        if (ttlMillis != null) {
            val duration = Duration.ofMillis(ttlMillis)
            jsonByKey.keys.forEach { key ->
                reactiveRedisTemplate.expire(key, duration).awaitSingle()
            }
        }
    }
}
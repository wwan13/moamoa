package server.cache

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.ScanOptions
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.types.Expiration
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component("redisCacheMemory")
internal class RedisCacheMemory(
    @param:Qualifier("cacheStringRedisTemplate")
    private val redis: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) : CacheMemory {

    private val valueOps get() = redis.opsForValue()

    override fun <T> get(key: String, type: Class<T>): T? {
        val json = runWithInfraException("Cache read failed. key=$key") { valueOps.get(key) } ?: return null
        return runCatching { objectMapper.readValue(json, type) }.getOrNull()
    }

    override fun <T> get(key: String, typeRef: TypeReference<T>): T? {
        val json = runWithInfraException("Cache read failed. key=$key") { valueOps.get(key) } ?: return null
        return runCatching { objectMapper.readValue(json, typeRef) }.getOrNull()
    }

    override fun <T> set(key: String, value: T, ttlMillis: Long?) {
        val json = objectMapper.writeValueAsString(value)
        runWithInfraException("Cache write failed. key=$key") {
            if (ttlMillis == null) valueOps.set(key, json)
            else valueOps.set(key, json, ttlMillis, TimeUnit.MILLISECONDS)
        }
    }

    override fun <T> setIfAbsent(key: String, value: T, ttlMillis: Long?): Boolean {
        val json = objectMapper.writeValueAsString(value)
        return runWithInfraException("Cache setIfAbsent failed. key=$key") {
            if (ttlMillis == null) valueOps.setIfAbsent(key, json) ?: false
            else valueOps.setIfAbsent(key, json, ttlMillis, TimeUnit.MILLISECONDS) ?: false
        }
    }

    override fun incr(key: String): Long =
        runWithInfraException("Cache increment failed. key=$key") { valueOps.increment(key) ?: 0L }

    override fun decrBy(key: String, delta: Long): Long =
        runWithInfraException("Cache decrement failed. key=$key delta=$delta") { valueOps.increment(key, -delta) ?: 0L }

    override fun evict(key: String) {
        runWithInfraException("Cache evict failed. key=$key") { redis.delete(key) }
    }

    override fun evictByPrefix(prefix: String) {
        val pattern = "$prefix*"
        val scanOptions = ScanOptions.scanOptions().match(pattern).count(500).build()
        runWithInfraException("Cache evictByPrefix failed. prefix=$prefix") {
            redis.scan(scanOptions).use { cursor ->
                while (cursor.hasNext()) {
                    redis.delete(cursor.next())
                }
            }
        }
    }

    override fun mget(keys: Collection<String>): Map<String, String?> {
        if (keys.isEmpty()) return emptyMap()
        val keyList = keys.toList()
        val values = runWithInfraException("Cache mget failed. size=${keyList.size}") {
            valueOps.multiGet(keyList) ?: emptyList()
        }
        return keyList.mapIndexed { idx, key -> key to values.getOrNull(idx) }.toMap()
    }

    override fun <T> mgetAs(keys: Collection<String>, typeRef: TypeReference<T>): Map<String, T?> {
        val raw = mget(keys)
        if (raw.isEmpty()) return emptyMap()
        return raw.mapValues { (_, json) ->
            if (json == null) null else runCatching { objectMapper.readValue(json, typeRef) }.getOrNull()
        }
    }

    override fun mset(valuesByKey: Map<String, Any>, ttlMillis: Long?) {
        if (valuesByKey.isEmpty()) return
        runWithInfraException("Cache mset failed. size=${valuesByKey.size}") {
            val jsonByKey = valuesByKey.mapValues { (_, v) -> objectMapper.writeValueAsString(v) }
            valueOps.multiSet(jsonByKey)
            if (ttlMillis != null) {
                val expiration = Expiration.milliseconds(ttlMillis)
                for (key in jsonByKey.keys) {
                    redis.expire(key, expiration.expirationTimeInMilliseconds, TimeUnit.MILLISECONDS)
                }
            }
        }
    }

    private inline fun <T> runWithInfraException(message: String, block: () -> T): T {
        return try {
            block()
        } catch (ex: Throwable) {
            if (ex is CacheInfraException) throw ex
            throw CacheInfraException(message, ex)
        }
    }
}
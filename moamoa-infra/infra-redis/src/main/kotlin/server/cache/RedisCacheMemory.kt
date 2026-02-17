package server.cache

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.data.redis.connection.ReactiveStringCommands.SetCommand
import org.springframework.data.redis.connection.RedisStringCommands.SetOption
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.ScanOptions
import org.springframework.data.redis.core.types.Expiration
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import server.shared.cache.CacheMemory
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.time.Duration

@Component("redisCacheMemory")
internal class RedisCacheMemory(
    private val reactiveRedisTemplate: ReactiveRedisTemplate<String, String>,
    private val objectMapper: ObjectMapper
) : CacheMemory {

    private val valueOps get() = reactiveRedisTemplate.opsForValue()

    final suspend inline fun <reified T> get(key: String): T? =
        get(key, object : TypeReference<T>() {})

    override suspend fun <T> get(key: String, type: Class<T>): T? {
        val json = valueOps.get(key).awaitFirstOrNull() ?: return null
        return runCatching { objectMapper.readValue(json, type) }.getOrNull()
    }

    override suspend fun <T> get(key: String, typeRef: TypeReference<T>): T? {
        val json = valueOps.get(key).awaitFirstOrNull() ?: return null
        return runCatching { objectMapper.readValue(json, typeRef) }.getOrNull()
    }

    override suspend fun <T> set(key: String, value: T, ttlMillis: Long?) {
        val json = objectMapper.writeValueAsString(value)
        if (ttlMillis == null) valueOps.set(key, json).awaitSingle()
        else valueOps.set(key, json, Duration.ofMillis(ttlMillis)).awaitSingle()
    }

    override suspend fun <T> setIfAbsent(key: String, value: T, ttlMillis: Long?): Boolean {
        val json = objectMapper.writeValueAsString(value)
        return if (ttlMillis == null) {
            valueOps.setIfAbsent(key, json).awaitSingle() ?: false
        } else {
            valueOps.setIfAbsent(key, json, Duration.ofMillis(ttlMillis)).awaitSingle() ?: false
        }
    }

    override suspend fun incr(key: String): Long =
        valueOps.increment(key).awaitSingle()

    override suspend fun decrBy(key: String, delta: Long): Long =
        valueOps.increment(key, -delta).awaitSingle()

    override suspend fun evict(key: String) {
        reactiveRedisTemplate.delete(key).awaitSingle()
    }

    override suspend fun evictByPrefix(prefix: String) {
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

    override suspend fun mget(keys: Collection<String>): Map<String, String?> {
        if (keys.isEmpty()) return emptyMap()
        val keyList = keys.toList()
        val values = reactiveRedisTemplate.execute { connection ->
            connection.stringCommands()
                .mGet(keyList.map(::encode))
                .map { buffers -> buffers.map { buffer -> buffer?.let(::decode) } }
        }.awaitFirstOrNull() ?: emptyList()

        return keyList.mapIndexed { idx, key -> key to values.getOrNull(idx) }.toMap()
    }

    final suspend inline fun <reified T> mgetAs(keys: Collection<String>): Map<String, T?> =
        mgetAs(keys, object : TypeReference<T>() {})

    override suspend fun <T> mgetAs(keys: Collection<String>, typeRef: TypeReference<T>): Map<String, T?> {
        val raw = mget(keys)
        if (raw.isEmpty()) return emptyMap()

        return raw.mapValues { (_, json) ->
            if (json == null) null
            else runCatching { objectMapper.readValue(json, typeRef) }.getOrNull()
        }
    }

    override suspend fun mset(valuesByKey: Map<String, Any>, ttlMillis: Long?) {
        if (valuesByKey.isEmpty()) return

        val jsonByKey = valuesByKey.mapValues { (_, v) -> objectMapper.writeValueAsString(v) }
        val encoded = LinkedHashMap<ByteBuffer, ByteBuffer>(jsonByKey.size)
        jsonByKey.forEach { (key, value) ->
            encoded[encode(key)] = encode(value)
        }

        if (ttlMillis == null) {
            reactiveRedisTemplate
                .execute { connection -> connection.stringCommands().mSet(encoded) }
                .awaitFirstOrNull()
            return
        }

        val ttl = Expiration.milliseconds(ttlMillis)
        val commands = Flux.fromIterable(jsonByKey.entries)
            .map { (key, value) ->
                SetCommand.set(encode(key))
                    .value(encode(value))
                    .expiring(ttl)
                    .withSetOption(SetOption.UPSERT)
            }

        reactiveRedisTemplate
            .execute { connection ->
                connection.stringCommands()
                    .set(commands)
                    .all { response -> response.isPresent && response.output == true }
            }
            .awaitFirstOrNull()
    }

    private fun encode(value: String): ByteBuffer =
        StandardCharsets.UTF_8.encode(value)

    private fun decode(value: ByteBuffer): String =
        StandardCharsets.UTF_8.decode(value.asReadOnlyBuffer()).toString()

}

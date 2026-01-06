package server.queue

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Component

@Component
class QueueMemory(
    private val redis: ReactiveRedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) {
    private val ops = redis.opsForList()

    suspend fun rPush(key: String, value: Any): Long {
        val json = objectMapper.writeValueAsString(value)
        return ops.rightPush(key, json).awaitSingle()
    }

    suspend fun lPush(key: String, value: Any): Long {
        val json = objectMapper.writeValueAsString(value)
        return ops.leftPush(key, json).awaitSingle()
    }

    suspend fun rPushAll(key: String, values: Collection<Any>): Long {
        if (values.isEmpty()) return len(key)

        val jsons = values.map { objectMapper.writeValueAsString(it) }
        return ops.rightPushAll(key, jsons).awaitSingle()
    }

    suspend fun lPushAll(key: String, values: Collection<Any>): Long {
        if (values.isEmpty()) return len(key)

        val jsons = values.map { objectMapper.writeValueAsString(it) }
        return ops.leftPushAll(key, jsons).awaitSingle()
    }

    final suspend inline fun <reified T> lPop(key: String): T? = lPop(key, T::class.java)
    final suspend inline fun <reified T> rPop(key: String): T? = rPop(key, T::class.java)

    suspend fun <T> lPop(key: String, type: Class<T>): T? {
        val json = ops.leftPop(key).awaitFirstOrNull() ?: return null
        return runCatching { objectMapper.readValue(json, type) }.getOrNull()
    }

    suspend fun <T> rPop(key: String, type: Class<T>): T? {
        val json = ops.rightPop(key).awaitFirstOrNull() ?: return null
        return runCatching { objectMapper.readValue(json, type) }.getOrNull()
    }

    final suspend inline fun <reified T> drain(key: String, max: Int = 1000): List<T> =
        drain(key, T::class.java, max)

    suspend fun <T> drain(key: String, type: Class<T>, max: Int = 1000): List<T> {
        val result = ArrayList<T>(minOf(max, 1000))
        repeat(max) {
            val v = lPop(key, type) ?: return result
            result.add(v)
        }
        return result
    }

    suspend fun len(key: String): Long =
        ops.size(key).awaitSingle()

    suspend fun delete(key: String) {
        redis.delete(key).awaitSingle()
    }
}
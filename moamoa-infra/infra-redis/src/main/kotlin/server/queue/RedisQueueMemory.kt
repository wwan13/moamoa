package server.queue

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Component
import server.shared.queue.QueueMemory

@Component
internal class RedisQueueMemory(
    private val redis: ReactiveRedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) : QueueMemory {
    private val ops = redis.opsForList()

    override suspend fun rPush(key: String, value: Any): Long {
        val json = objectMapper.writeValueAsString(value)
        return ops.rightPush(key, json).awaitSingle()
    }

    override suspend fun lPush(key: String, value: Any): Long {
        val json = objectMapper.writeValueAsString(value)
        return ops.leftPush(key, json).awaitSingle()
    }

    override suspend fun rPushAll(key: String, values: Collection<Any>): Long {
        if (values.isEmpty()) return len(key)

        val jsons = values.map { objectMapper.writeValueAsString(it) }
        return ops.rightPushAll(key, jsons).awaitSingle()
    }

    override suspend fun lPushAll(key: String, values: Collection<Any>): Long {
        if (values.isEmpty()) return len(key)

        val jsons = values.map { objectMapper.writeValueAsString(it) }
        return ops.leftPushAll(key, jsons).awaitSingle()
    }

    override suspend fun <T> lPop(key: String, type: Class<T>): T? {
        val json = ops.leftPop(key).awaitFirstOrNull() ?: return null
        return runCatching { objectMapper.readValue(json, type) }.getOrNull()
    }

    override suspend fun <T> rPop(key: String, type: Class<T>): T? {
        val json = ops.rightPop(key).awaitFirstOrNull() ?: return null
        return runCatching { objectMapper.readValue(json, type) }.getOrNull()
    }

    override suspend fun <T> drain(key: String, type: Class<T>, max: Int): List<T> {
        val result = ArrayList<T>(minOf(max, 1000))
        repeat(max) {
            val v = lPop(key, type) ?: return result
            result.add(v)
        }
        return result
    }

    override suspend fun len(key: String): Long =
        ops.size(key).awaitSingle()

    override suspend fun delete(key: String) {
        redis.delete(key).awaitSingle()
    }
}

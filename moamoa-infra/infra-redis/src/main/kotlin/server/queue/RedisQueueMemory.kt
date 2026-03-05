package server.queue

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import server.queue.QueueMemory

@Component
internal class RedisQueueMemory(
    @param:Qualifier("cacheStringRedisTemplate")
    private val redis: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) : QueueMemory {
    private val ops = redis.opsForList()

    override fun rPush(key: String, value: Any): Long =
        ops.rightPush(key, objectMapper.writeValueAsString(value)) ?: 0L

    override fun lPush(key: String, value: Any): Long =
        ops.leftPush(key, objectMapper.writeValueAsString(value)) ?: 0L

    override fun rPushAll(key: String, values: Collection<Any>): Long {
        if (values.isEmpty()) return len(key)
        return ops.rightPushAll(key, values.map(objectMapper::writeValueAsString)) ?: 0L
    }

    override fun lPushAll(key: String, values: Collection<Any>): Long {
        if (values.isEmpty()) return len(key)
        return ops.leftPushAll(key, values.map(objectMapper::writeValueAsString)) ?: 0L
    }

    override fun <T> lPop(key: String, type: Class<T>): T? =
        ops.leftPop(key)?.let { runCatching { objectMapper.readValue(it, type) }.getOrNull() }

    override fun <T> rPop(key: String, type: Class<T>): T? =
        ops.rightPop(key)?.let { runCatching { objectMapper.readValue(it, type) }.getOrNull() }

    override fun <T> drain(key: String, type: Class<T>, max: Int): List<T> {
        val result = ArrayList<T>(minOf(max, 1000))
        repeat(max) {
            val value = lPop(key, type) ?: return result
            result.add(value)
        }
        return result
    }

    override fun len(key: String): Long = ops.size(key) ?: 0L

    override fun delete(key: String) {
        redis.delete(key)
    }
}
package server.messaging.read

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.connection.stream.ReadOffset
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import server.messaging.definition.EventStream
import java.util.concurrent.ConcurrentHashMap

@Component
internal class StreamGroupEnsurer(
    @param:Qualifier("streamStringRedisTemplate")
    private val redis: StringRedisTemplate,
) {
    private data class EnsureKey(val channelKey: String, val consumerGroup: String)

    private val ensured = ConcurrentHashMap.newKeySet<EnsureKey>()

    fun ensure(stream: EventStream) {
        val ensureKey = EnsureKey(stream.channel.key, stream.consumerGroup)
        ensureGroupOnce(stream, ensureKey)
    }

    fun ensureForRecovery(stream: EventStream) {
        val ensureKey = EnsureKey(stream.channel.key, stream.consumerGroup)
        ensured.remove(ensureKey)
        ensureGroupOnce(stream, ensureKey)
    }

    private fun ensureGroupOnce(
        stream: EventStream,
        ensureKey: EnsureKey,
    ) {
        if (!ensured.add(ensureKey)) return
        try {
            redis.opsForStream<String, String>()
                .createGroup(stream.channel.key, ReadOffset.from("0-0"), stream.consumerGroup)
        } catch (_: Exception) {
            // BUSYGROUP 등은 정상 취급
        }
    }
}

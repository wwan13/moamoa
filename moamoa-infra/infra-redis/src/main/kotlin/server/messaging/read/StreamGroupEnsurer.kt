package server.messaging.read

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.connection.stream.ReadOffset
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import server.shared.messaging.SubscriptionDefinition
import java.util.concurrent.ConcurrentHashMap

@Component
internal class StreamGroupEnsurer(
    @param:Qualifier("streamStringRedisTemplate")
    private val redis: StringRedisTemplate,
) {
    private data class EnsureKey(val channelKey: String, val consumerGroup: String)

    private val ensured = ConcurrentHashMap.newKeySet<EnsureKey>()

    suspend fun ensure(subscription: SubscriptionDefinition) {
        val ensureKey = EnsureKey(subscription.channel.key, subscription.consumerGroup)
        ensureGroupOnce(subscription, ensureKey)
    }

    suspend fun ensureForRecovery(subscription: SubscriptionDefinition) {
        val ensureKey = EnsureKey(subscription.channel.key, subscription.consumerGroup)
        ensured.remove(ensureKey)
        ensureGroupOnce(subscription, ensureKey)
    }

    private suspend fun ensureGroupOnce(
        subscription: SubscriptionDefinition,
        ensureKey: EnsureKey,
    ) = withContext(Dispatchers.IO) {
        if (!ensured.add(ensureKey)) return@withContext
        try {
            redis.opsForStream<String, String>()
                .createGroup(subscription.channel.key, ReadOffset.from("0-0"), subscription.consumerGroup)
        } catch (_: Exception) {
            // BUSYGROUP 등은 정상 취급
        }
    }
}

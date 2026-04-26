package server.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "messaging.redis-health")
internal data class RedisHealthProperties(
    var pauseOnFailureMs: Long = 30_000,
    var recoveryProbeIntervalMs: Long = 30_000,
)

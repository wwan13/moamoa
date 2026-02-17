package server.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "cache.resilience")
internal data class ResilientCacheProperties(
    var recoveryProbeIntervalMs: Long = 30_000,
)

package server.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "lock.resilience")
internal data class ResilientLockProperties(
    var recoveryProbeIntervalMs: Long = 30_000,
)

package server.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "messaging.subscription-reader")
internal data class SubscriptionReaderProperties(
    var readPauseOnFailureMs: Long = 30_000,
    var recoveryProbeIntervalMs: Long = 5_000,
)

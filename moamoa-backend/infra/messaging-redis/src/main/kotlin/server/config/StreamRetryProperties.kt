package server.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "messaging.stream-retry")
internal data class StreamRetryProperties(
    var intervalMs: Long = 60_000,
)

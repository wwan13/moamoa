package server.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "webhook")
internal data class WebhookProperties(
    val error: String,
    val service: String,
    val batch: String
)
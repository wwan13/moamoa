package server.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "mail")
internal data class MailProperties(
    val apiKey: String,
    val domain: String,
    val from: String
)
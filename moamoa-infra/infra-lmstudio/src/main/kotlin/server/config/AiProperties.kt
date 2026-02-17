package server.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "ai")
internal data class AiProperties(
    val secretKey: String,
    val model: String,
    val baseUrl: String
)
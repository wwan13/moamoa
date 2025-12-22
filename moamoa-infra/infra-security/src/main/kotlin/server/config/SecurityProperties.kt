package server.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "security")
data class SecurityProperties(
    val secretKey: String
)

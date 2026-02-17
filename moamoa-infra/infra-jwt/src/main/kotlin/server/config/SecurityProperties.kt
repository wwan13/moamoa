package server.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "security")
internal data class SecurityProperties(
    val secretKey: String
)

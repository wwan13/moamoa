package server.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "playwright")
internal data class PlaywrightProperties(
    val wsEndpoint: String? = null,
)

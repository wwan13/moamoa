package server.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "redis")
internal data class RedisProperties(
    val host: String,
    val port: Int,
    val database: Int,
    val username: String?,
    val password: String?,
    val timeout: Duration,
)

package server.admin.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "admin.datasource")
internal data class AdminLogDatasourceProperties(
    val url: String? = null,
    val username: String? = null,
    val password: String? = null,
    val driverClassName: String = "com.mysql.cj.jdbc.Driver",
    val maximumPoolSize: Int = 5,
    val minimumIdle: Int = 1,
    val connectionTimeoutMs: Long = 3000,
    val idleTimeoutMs: Long = 600_000,
    val maxLifetimeMs: Long = 1_800_000,
)

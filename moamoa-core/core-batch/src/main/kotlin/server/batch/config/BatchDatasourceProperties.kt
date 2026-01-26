package server.batch.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "spring.datasource")
internal data class BatchDatasourceProperties(
    val url: String,
    val username: String,
    val password: String
)
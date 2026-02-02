package server.batch.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "batch")
data class BatchRunKeyProperties(
    val runKey: String,
)

package server.techblog.http

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "tech-blog.crawler")
internal data class TechBlogCrawlerProperties(
    val baseUrl: String,
    val connectTimeout: Duration,
    val responseTimeout: Duration,
    val maxInMemorySize: Int,
)

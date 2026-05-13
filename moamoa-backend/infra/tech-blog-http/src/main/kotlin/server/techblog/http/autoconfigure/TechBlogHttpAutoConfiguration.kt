package server.techblog.http.autoconfigure

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.PropertySource

@AutoConfiguration
@PropertySource(
    value = ["classpath:application-tech-blog-http.yml"],
    factory = YamlPropertySourceFactory::class,
)
@Import(TechBlogCrawlerClientConfig::class)
class TechBlogHttpAutoConfiguration

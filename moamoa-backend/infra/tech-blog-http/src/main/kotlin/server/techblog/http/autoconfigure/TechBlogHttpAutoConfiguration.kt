package server.techblog.http.autoconfigure

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Import

@AutoConfiguration
@Import(TechBlogCrawlerClientConfig::class)
class TechBlogHttpAutoConfiguration

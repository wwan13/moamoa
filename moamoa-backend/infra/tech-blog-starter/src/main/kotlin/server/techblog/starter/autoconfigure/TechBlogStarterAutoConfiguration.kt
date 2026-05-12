package server.techblog.starter.autoconfigure

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Import
import server.techblog.TechBlogPostCatetorizer
import server.techblog.http.autoconfigure.TechBlogHttpAutoConfiguration

@AutoConfiguration
@Import(
    TechBlogHttpAutoConfiguration::class,
    TechBlogPostCatetorizer::class,
)
class TechBlogStarterAutoConfiguration

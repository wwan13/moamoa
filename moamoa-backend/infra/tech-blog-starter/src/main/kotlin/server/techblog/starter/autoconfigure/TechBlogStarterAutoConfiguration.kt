package server.techblog.starter.autoconfigure

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Import
import server.techblog.TechBlogPostCatetorizer
import server.techblog.TechBlogSources
import server.techblog.http.autoconfigure.TechBlogHttpAutoConfiguration
import server.techblog.jsoup.autoconfigure.TechBlogJsoupAutoConfiguration
import server.techblog.playwright.autoconfigure.TechBlogPlaywrightAutoConfiguration

@AutoConfiguration
@Import(
    TechBlogHttpAutoConfiguration::class,
    TechBlogJsoupAutoConfiguration::class,
    TechBlogPlaywrightAutoConfiguration::class,
    TechBlogSources::class,
    TechBlogPostCatetorizer::class,
)
class TechBlogStarterAutoConfiguration

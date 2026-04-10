package server.techblog.jsoup.autoconfigure

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.ComponentScan

@AutoConfiguration
@ComponentScan(basePackages = ["server.source.jsoup"])
class TechBlogJsoupAutoConfiguration

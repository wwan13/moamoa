package server.techblog.http.autoconfigure

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.ComponentScan

@AutoConfiguration
@ComponentScan(basePackages = ["server.config", "server.source.http"])
class TechBlogHttpAutoConfiguration

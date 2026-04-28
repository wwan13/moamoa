package server.techblog.playwright.autoconfigure

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.ComponentScan

@AutoConfiguration
@ComponentScan(basePackages = ["server.source.playwright"])
class TechBlogPlaywrightAutoConfiguration

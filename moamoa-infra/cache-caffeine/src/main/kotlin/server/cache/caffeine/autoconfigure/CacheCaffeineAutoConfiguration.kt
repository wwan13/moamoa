package server.cache.caffeine.autoconfigure

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Import
import server.cache.CaffeineCacheMemory

@AutoConfiguration
@ConditionalOnMissingBean(name = ["caffeineCacheMemory"])
@Import(CaffeineCacheMemory::class)
class CacheCaffeineAutoConfiguration

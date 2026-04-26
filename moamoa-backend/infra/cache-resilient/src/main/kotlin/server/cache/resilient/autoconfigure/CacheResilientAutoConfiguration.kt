package server.cache.resilient.autoconfigure

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Import
import server.cache.ResilientCacheAspect
import server.cache.ResilientCacheMemory
import server.cache.ResilientCacheMethodInvoker
import server.cache.ResilientCacheRouter
import server.config.ResilientCacheProperties

@AutoConfiguration
@ConditionalOnMissingBean(name = ["resilientCacheMemoryProxy"])
@Import(
    ResilientCacheProperties::class,
    ResilientCacheRouter::class,
    ResilientCacheMethodInvoker::class,
    ResilientCacheMemory::class,
    ResilientCacheAspect::class,
)
class CacheResilientAutoConfiguration

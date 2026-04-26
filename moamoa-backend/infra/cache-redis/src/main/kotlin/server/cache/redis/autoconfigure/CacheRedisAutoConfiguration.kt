package server.cache.redis.autoconfigure

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Import
import server.cache.RedisCacheMemory
import server.cache.logging.CacheLoggingAspect
import server.config.CacheObjectMapperConfig
import server.config.CacheRedisConfig

@AutoConfiguration
@ConditionalOnMissingBean(name = ["redisCacheMemory"])
@Import(
    CacheRedisConfig::class,
    CacheObjectMapperConfig::class,
    RedisCacheMemory::class,
    CacheLoggingAspect::class,
)
class CacheRedisAutoConfiguration

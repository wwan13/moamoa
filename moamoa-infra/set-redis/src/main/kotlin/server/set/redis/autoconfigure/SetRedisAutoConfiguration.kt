package server.set.redis.autoconfigure

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Import
import server.config.SetRedisConfig
import server.set.RedisSetMemory

@AutoConfiguration
@ConditionalOnMissingBean(name = ["redisSetMemory"])
@Import(
    SetRedisConfig::class,
    RedisSetMemory::class,
)
class SetRedisAutoConfiguration

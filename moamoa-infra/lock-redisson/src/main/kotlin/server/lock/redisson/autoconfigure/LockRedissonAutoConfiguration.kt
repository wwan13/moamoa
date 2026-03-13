package server.lock.redisson.autoconfigure

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Import
import server.config.LockRedisConfig
import server.lock.RedissonLock

@AutoConfiguration
@ConditionalOnMissingBean(name = ["redissonLock"])
@Import(
    LockRedisConfig::class,
    RedissonLock::class,
)
class LockRedissonAutoConfiguration

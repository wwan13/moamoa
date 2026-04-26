package server.queue.redis.autoconfigure

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Import
import server.config.QueueObjectMapperConfig
import server.config.QueueRedisConfig
import server.queue.RedisQueueMemory

@AutoConfiguration
@ConditionalOnMissingBean(name = ["redisQueueMemory"])
@Import(
    QueueRedisConfig::class,
    QueueObjectMapperConfig::class,
    RedisQueueMemory::class,
)
class QueueRedisAutoConfiguration

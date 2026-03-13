package server.messaging.redis.autoconfigure

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Import
import server.config.MessagingConfig
import server.config.MessagingRedisConfig
import server.config.RedisHealthProperties
import server.config.StreamRetryProperties
import server.messaging.MessageHandlerInvoker
import server.messaging.StreamEventHandlers
import server.messaging.StreamEventPublisher
import server.messaging.health.RedisHealthStateManager
import server.messaging.health.RedisRecoveryActionRunner
import server.messaging.logging.EventPublisherLoggingAspect
import server.messaging.logging.MessageHandlerInvokerAspect
import server.messaging.read.StreamGroupEnsurer
import server.messaging.read.StreamMessageProcessor
import server.messaging.read.StreamReader
import server.messaging.retry.StreamRetrier
import server.messaging.retry.StreamRetryProcessor

@AutoConfiguration
@ConditionalOnMissingBean(name = ["streamEventPublisher"])
@EnableConfigurationProperties(
    RedisHealthProperties::class,
    StreamRetryProperties::class,
)
@Import(
    MessagingRedisConfig::class,
    MessagingConfig::class,
    MessageHandlerInvoker::class,
    StreamEventHandlers::class,
    StreamEventPublisher::class,
    RedisHealthStateManager::class,
    RedisRecoveryActionRunner::class,
    EventPublisherLoggingAspect::class,
    MessageHandlerInvokerAspect::class,
    StreamGroupEnsurer::class,
    StreamMessageProcessor::class,
    StreamReader::class,
    StreamRetrier::class,
    StreamRetryProcessor::class,
)
class MessagingRedisAutoConfiguration

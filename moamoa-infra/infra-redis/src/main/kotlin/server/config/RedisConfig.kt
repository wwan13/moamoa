package server.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.connection.RedisPassword
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration

@Configuration
@ConfigurationPropertiesScan
internal class RedisConfig {

    @Bean
    @Primary
    fun cacheRedisConnectionFactory(properties: RedisProperties): LettuceConnectionFactory =
        buildConnectionFactory(properties, Duration.ofSeconds(2))

    @Bean
    fun streamRedisConnectionFactory(properties: RedisProperties): LettuceConnectionFactory =
        buildConnectionFactory(properties, Duration.ofSeconds(30))

    @Bean
    @Primary
    fun cacheReactiveRedisTemplate(
        @Qualifier("cacheRedisConnectionFactory") connectionFactory: ReactiveRedisConnectionFactory,
    ): ReactiveRedisTemplate<String, String> =
        ReactiveRedisTemplate(connectionFactory, stringContext())

    @Bean
    fun streamReactiveRedisTemplate(
        @Qualifier("streamRedisConnectionFactory") connectionFactory: ReactiveRedisConnectionFactory,
    ): ReactiveRedisTemplate<String, String> =
        ReactiveRedisTemplate(connectionFactory, stringContext())

    @Bean
    fun streamStringRedisTemplate(
        @Qualifier("streamRedisConnectionFactory") connectionFactory: LettuceConnectionFactory,
    ): StringRedisTemplate = StringRedisTemplate(connectionFactory)

    private fun buildConnectionFactory(
        properties: RedisProperties,
        commandTimeout: Duration,
    ): LettuceConnectionFactory {
        val standalone = RedisStandaloneConfiguration(properties.host, properties.port).apply {
            database = properties.database
            if (!properties.username.isNullOrBlank()) {
                username = properties.username
            }
            if (!properties.password.isNullOrBlank()) {
                password = RedisPassword.of(properties.password)
            }
        }

        val clientConfig = LettuceClientConfiguration.builder()
            .commandTimeout(commandTimeout)
            .build()

        return LettuceConnectionFactory(standalone, clientConfig).apply { afterPropertiesSet() }
    }

    private fun stringContext(): RedisSerializationContext<String, String> {
        val serializer = StringRedisSerializer()
        return RedisSerializationContext.newSerializationContext<String, String>(serializer)
            .key(serializer)
            .value(serializer)
            .hashKey(serializer)
            .hashValue(serializer)
            .build()
    }
}

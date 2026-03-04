package server.config

import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.connection.RedisPassword
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate
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
    fun cacheStringRedisTemplate(
        @Qualifier("cacheRedisConnectionFactory") connectionFactory: LettuceConnectionFactory,
    ): StringRedisTemplate = StringRedisTemplate(connectionFactory)

    @Bean
    fun streamStringRedisTemplate(
        @Qualifier("streamRedisConnectionFactory") connectionFactory: LettuceConnectionFactory,
    ): StringRedisTemplate = StringRedisTemplate(connectionFactory)

    @Bean(destroyMethod = "shutdown")
    fun redissonClient(properties: RedisProperties): RedissonClient =
        Redisson.create(redissonConfig(properties))

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

    private fun redissonConfig(properties: RedisProperties): Config {
        val config = Config()
        config.useSingleServer()
            .setAddress(buildRedisAddress(properties))
            .setDatabase(properties.database)
        return config
    }

    private fun buildRedisAddress(properties: RedisProperties): String {
        val username = properties.username?.takeIf { it.isNotBlank() }
        val password = properties.password?.takeIf { it.isNotBlank() }

        val authPart = when {
            username != null && password != null -> "$username:$password@"
            username != null -> "$username@"
            password != null -> ":$password@"
            else -> ""
        }

        return "redis://$authPart${properties.host}:${properties.port}"
    }
}

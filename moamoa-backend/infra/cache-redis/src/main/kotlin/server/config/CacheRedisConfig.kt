package server.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisPassword
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate

@Configuration
@EnableConfigurationProperties(CacheRedisProperties::class)
internal class CacheRedisConfig {

    @Bean
    fun cacheRedisConnectionFactory(properties: CacheRedisProperties): RedisConnectionFactory {
        val standalone = RedisStandaloneConfiguration(properties.host, properties.port).apply {
            database = properties.database
            if (!properties.username.isNullOrBlank()) username = properties.username
            if (!properties.password.isNullOrBlank()) password = RedisPassword.of(properties.password)
        }
        val clientConfig = LettuceClientConfiguration.builder()
            .commandTimeout(properties.timeout)
            .build()
        return LettuceConnectionFactory(standalone, clientConfig).apply { afterPropertiesSet() }
    }

    @Bean
    fun cacheStringRedisTemplate(
        @Qualifier("cacheRedisConnectionFactory") connectionFactory: RedisConnectionFactory,
    ): StringRedisTemplate = StringRedisTemplate(connectionFactory)
}

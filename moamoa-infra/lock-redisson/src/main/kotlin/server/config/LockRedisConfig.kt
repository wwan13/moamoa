package server.config

import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(LockRedisProperties::class)
internal class LockRedisConfig {

    @Bean(destroyMethod = "shutdown")
    fun redissonClient(properties: LockRedisProperties): RedissonClient =
        Redisson.create(redissonConfig(properties))

    private fun redissonConfig(properties: LockRedisProperties): Config {
        val config = Config()
        config.useSingleServer()
            .setAddress(buildRedisAddress(properties))
            .setDatabase(properties.database)
        return config
    }

    private fun buildRedisAddress(properties: LockRedisProperties): String {
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

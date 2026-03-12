package server.admin.global.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import javax.sql.DataSource
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.PlatformTransactionManager

@Configuration
@EnableConfigurationProperties(AdminLogDatasourceProperties::class)
@ConditionalOnProperty(prefix = "admin.datasource", name = ["enabled"], havingValue = "true")
internal class AdminLogDatasourceConfig {

    @Bean("adminLogDataSource")
    fun adminLogDataSource(properties: AdminLogDatasourceProperties): DataSource {
        val url = requireNotNull(properties.url) { "admin.datasource.url must be set when admin datasource is enabled." }
        val username = requireNotNull(properties.username) { "admin.datasource.username must be set when admin datasource is enabled." }
        val password = requireNotNull(properties.password) { "admin.datasource.password must be set when admin datasource is enabled." }

        val hikariConfig = HikariConfig().apply {
            jdbcUrl = url
            this.username = username
            this.password = password
            driverClassName = properties.driverClassName
            maximumPoolSize = properties.maximumPoolSize
            minimumIdle = properties.minimumIdle
            connectionTimeout = properties.connectionTimeoutMs
            idleTimeout = properties.idleTimeoutMs
            maxLifetime = properties.maxLifetimeMs
            poolName = "admin-log-datasource"
        }

        return HikariDataSource(hikariConfig)
    }

    @Bean("adminLogJdbcTemplate")
    fun adminLogJdbcTemplate(adminLogDataSource: DataSource): JdbcTemplate = JdbcTemplate(adminLogDataSource)

    @Bean("adminLogNamedParameterJdbcTemplate")
    fun adminLogNamedParameterJdbcTemplate(adminLogDataSource: DataSource): NamedParameterJdbcTemplate {
        return NamedParameterJdbcTemplate(adminLogDataSource)
    }

    @Bean("adminLogTransactionManager")
    fun adminLogTransactionManager(adminLogDataSource: DataSource): PlatformTransactionManager {
        return DataSourceTransactionManager(adminLogDataSource)
    }
}

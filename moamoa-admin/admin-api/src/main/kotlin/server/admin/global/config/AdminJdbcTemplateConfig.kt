package server.admin.global.config

import javax.sql.DataSource
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

@Configuration
internal class AdminJdbcTemplateConfig {

    @Bean("jdbcTemplate")
    @Primary
    @ConditionalOnMissingBean(name = ["jdbcTemplate"])
    fun jdbcTemplate(dataSource: DataSource): JdbcTemplate = JdbcTemplate(dataSource)

    @Bean("namedParameterJdbcTemplate")
    @Primary
    @ConditionalOnMissingBean(name = ["namedParameterJdbcTemplate"])
    fun namedParameterJdbcTemplate(dataSource: DataSource): NamedParameterJdbcTemplate {
        return NamedParameterJdbcTemplate(dataSource)
    }
}


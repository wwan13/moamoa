package server.batch.config

import org.springframework.boot.autoconfigure.batch.BatchDataSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import javax.sql.DataSource

@Configuration
internal class BatchDatasourceConfig(
    private val props: BatchDatasourceProperties
) {

    @BatchDataSource
    @Bean(name = ["dataSource"])
    fun batchDataSource(): DataSource {
        return DriverManagerDataSource().apply {
            setDriverClassName("com.mysql.cj.jdbc.Driver")
            setUrl(props.url)
            setUsername(props.username)
            setPassword(props.password)
        }
    }

    @Primary
    @Bean(name = ["jdbcTemplate"])
    fun jdbcTemplate(
        @Qualifier("dataSource") dataSource: DataSource
    ): JdbcTemplate = JdbcTemplate(dataSource)

    @Primary
    @Bean(name = ["namedParameterJdbcTemplate"])
    fun namedParameterJdbcTemplate(
        @Qualifier("dataSource") dataSource: DataSource
    ): NamedParameterJdbcTemplate = NamedParameterJdbcTemplate(dataSource)
}

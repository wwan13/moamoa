package server.batch.config

import org.springframework.boot.autoconfigure.batch.BatchDataSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
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
}
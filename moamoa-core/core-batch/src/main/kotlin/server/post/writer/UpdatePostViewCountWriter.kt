package server.post.writer

import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider
import org.springframework.batch.item.database.JdbcBatchItemWriter
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import server.post.dto.PostViewCount
import javax.sql.DataSource

@Component
class UpdatePostViewCountWriter(
    private val dataSource: DataSource,
) {
    fun build() = JdbcBatchItemWriter<PostViewCount>().apply {
        setDataSource(dataSource)
        setSql(
            """
            UPDATE post
            SET view_count = :viewCount
            WHERE id = :postId
            """.trimIndent()
        )
        setItemSqlParameterSourceProvider(BeanPropertyItemSqlParameterSourceProvider())
        afterPropertiesSet()
    }
}
package server.batch.post.writer

import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider
import org.springframework.batch.item.database.JdbcBatchItemWriter
import org.springframework.stereotype.Component
import server.batch.post.dto.PostViewCount
import javax.sql.DataSource

@Component
internal class UpdatePostViewCountWriter(
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
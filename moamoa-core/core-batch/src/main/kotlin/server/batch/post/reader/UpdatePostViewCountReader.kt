package server.batch.post.reader

import org.springframework.batch.item.database.JdbcCursorItemReader
import org.springframework.jdbc.core.DataClassRowMapper
import org.springframework.stereotype.Component
import server.batch.post.dto.PostViewCount
import javax.sql.DataSource

@Component
internal class UpdatePostViewCountReader(
    private val dataSource: DataSource,
) {
    fun build() = JdbcCursorItemReader<PostViewCount>().apply {
        setName("updatePostViewCountReader")
        setDataSource(this@UpdatePostViewCountReader.dataSource)
        setSql(
            """
                SELECT p.id AS post_id, p.view_count AS view_count
                FROM post p
                """.trimIndent()
        )
        setRowMapper(DataClassRowMapper(PostViewCount::class.java))
    }
}
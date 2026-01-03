package server.techblog.reader

import org.springframework.batch.item.database.JdbcPagingItemReader
import org.springframework.batch.item.database.Order
import org.springframework.batch.item.database.support.MySqlPagingQueryProvider
import org.springframework.jdbc.core.DataClassRowMapper
import org.springframework.stereotype.Component
import server.techblog.dto.TechBlogKey
import javax.sql.DataSource

@Component
class FetchTechBlogPostsReader(
    private val dataSource: DataSource,
) {

    fun build(): JdbcPagingItemReader<TechBlogKey> {
        val queryProvider = MySqlPagingQueryProvider().apply {
            setSelectClause(
                """
                SELECT
                  t.id,
                  t.tech_blog_key
                """.trimIndent()
            )
            setFromClause("FROM tech_blog t")
            setSortKeys(mapOf("t.id" to Order.ASCENDING))
        }

        val reader = JdbcPagingItemReader<TechBlogKey>().apply {
            setName("fetchTechBlogPostsReader") // 중요
            setDataSource(dataSource)
            setQueryProvider(queryProvider)
            setPageSize(100)
            setRowMapper(DataClassRowMapper(TechBlogKey::class.java))
        }

        reader.afterPropertiesSet()
        return reader
    }
}
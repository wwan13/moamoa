package server.batch.post.reader

import org.springframework.batch.item.ItemReader
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import server.batch.post.dto.PostSummary
import javax.sql.DataSource

@Component
internal class CategorizingPostReader(
    dataSource: DataSource,
) : ItemReader<List<PostSummary>> {

    private val jdbc = NamedParameterJdbcTemplate(dataSource)
    private var alreadyRead = false

    override fun read(): List<PostSummary>? {
        if (alreadyRead) return null
        alreadyRead = true

        val sql = """
            SELECT
                p.id AS post_id,
                p.title,
                p.description,
                p.post_key,
                COALESCE(GROUP_CONCAT(t.title ORDER BY t.title SEPARATOR ','), '') AS tags
            FROM `post` p
            LEFT JOIN post_tag pt ON pt.post_id = p.id
            LEFT JOIN tag t ON t.id = pt.tag_id
            WHERE p.category_id = 999
            GROUP BY p.id
            ORDER BY p.id ASC
            LIMIT 10
        """.trimIndent()

        return jdbc.query(sql, emptyMap<String, Any>()) { rs, _ ->
            PostSummary(
                postId = rs.getLong("post_id"),
                title = rs.getString("title"),
                description = rs.getString("description"),
                key = rs.getString("post_key"),
                tags = rs.getString("tags")
                    ?.split(",")
                    ?.filter { it.isNotBlank() }
                    ?: emptyList()
            )
        }
    }
}
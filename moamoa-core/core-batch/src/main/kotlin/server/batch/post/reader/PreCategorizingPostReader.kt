package server.batch.post.reader

import org.springframework.batch.item.ItemReader
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import server.batch.post.dto.PostSummary
import javax.sql.DataSource

@Component
internal class PreCategorizingPostReader(
    dataSource: DataSource,
) : ItemReader<List<PostSummary>> {

    private val jdbc = NamedParameterJdbcTemplate(dataSource)
    private var lastId: Long? = null

    override fun read(): List<PostSummary>? {
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
              AND p.id > :lastId
            GROUP BY p.id
            ORDER BY p.id ASC
            LIMIT 100
        """.trimIndent()

        val items = jdbc.query(
            sql,
            mapOf("lastId" to (lastId ?: 0L))
        ) { rs, _ ->
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

        if (items.isEmpty()) return null
        lastId = items.last().postId
        return items
    }
}

package server.post.writer

import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import server.post.dto.PostCategory

@Component
class CategorizingPostWriter(
    private val jdbc: NamedParameterJdbcTemplate
) : ItemWriter<List<PostCategory>> {

    override fun write(chunk: Chunk<out List<PostCategory>>) {
        val items = chunk.flatten()
        if (items.isEmpty()) return

        val sql = """
            UPDATE post
            SET category_id = :categoryId
            WHERE id = :postId
        """.trimIndent()

        val params = items.map {
            mapOf(
                "postId" to it.postId,
                "categoryId" to it.categoryId
            )
        }.toTypedArray()

        jdbc.batchUpdate(sql, params)
    }
}
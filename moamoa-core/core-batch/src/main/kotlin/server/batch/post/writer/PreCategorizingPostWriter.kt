package server.batch.post.writer

import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import server.batch.common.transaction.AfterCommitExecutor
import server.batch.post.dto.PostCategory
import server.batch.post.dto.PostSummary
import server.batch.post.dto.PreCategorizingPostResult
import server.shared.queue.QueueMemory

@Component
internal class PreCategorizingPostWriter(
    private val jdbc: NamedParameterJdbcTemplate,
    private val afterCommitExecutor: AfterCommitExecutor,
    private val queueMemory: QueueMemory,
) : ItemWriter<PreCategorizingPostResult> {

    override fun write(chunk: Chunk<out PreCategorizingPostResult>) {
        val categorized = chunk.flatMap { it.categorized }
        val uncategorized = chunk.flatMap { it.uncategorized }

        updateCategories(categorized)
        enqueueUncategorized(uncategorized)
    }

    private fun updateCategories(items: List<PostCategory>) {
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

    private fun enqueueUncategorized(items: List<PostSummary>) {
        if (items.isEmpty()) return

        afterCommitExecutor.execute {
            queueMemory.rPushAll("AI_CATEGORIZING_POSTS", items)
        }
    }
}

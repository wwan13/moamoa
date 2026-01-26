package server.batch.post.tasklet

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus

@Component
internal class SyncPostBookmarkCountTasklet(
    private val jdbcTemplate: JdbcTemplate
) : Tasklet {

    override fun execute(
        contribution: StepContribution,
        chunkContext: ChunkContext
    ): RepeatStatus {
        val updated = jdbcTemplate.update(
            """
            UPDATE post p
            LEFT JOIN (
                SELECT post_id, COUNT(*) AS cnt
                FROM post_bookmark
                GROUP BY post_id
            ) x ON x.post_id = p.id
            SET p.bookmark_count = COALESCE(x.cnt, 0)
            """.trimIndent()
        )

        contribution.incrementWriteCount(updated.toLong())
        return RepeatStatus.FINISHED
    }
}
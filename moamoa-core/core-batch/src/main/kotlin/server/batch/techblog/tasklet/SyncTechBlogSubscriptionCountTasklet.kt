package server.batch.techblog.tasklet

import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
internal class SyncTechBlogSubscriptionCountTasklet(
    private val jdbcTemplate: JdbcTemplate
) : Tasklet {

    override fun execute(
        contribution: StepContribution,
        chunkContext: ChunkContext
    ): RepeatStatus {
        val updated = jdbcTemplate.update(
            """
            UPDATE tech_blog tb
            LEFT JOIN (
                SELECT tech_blog_id, COUNT(*) AS cnt
                FROM tech_blog_subscription
                GROUP BY tech_blog_id
            ) x ON x.tech_blog_id = tb.id
            SET tb.subscription_count = COALESCE(x.cnt, 0)
            """.trimIndent()
        )

        contribution.incrementWriteCount(updated.toLong())
        return RepeatStatus.FINISHED
    }
}
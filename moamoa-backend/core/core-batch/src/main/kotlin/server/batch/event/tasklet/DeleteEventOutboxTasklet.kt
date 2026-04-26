package server.batch.event.tasklet

import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
internal class DeleteEventOutboxTasklet(
    private val jdbcTemplate: JdbcTemplate
) : Tasklet {

    override fun execute(
        contribution: StepContribution,
        chunkContext: ChunkContext
    ): RepeatStatus {
        val deleted = jdbcTemplate.update(
            """
            DELETE FROM event_outbox
            """.trimIndent()
        )

        contribution.incrementWriteCount(deleted.toLong())
        return RepeatStatus.FINISHED
    }
}

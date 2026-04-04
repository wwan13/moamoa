package server.batch.event.deleteeventoutbox.job

import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Component
import server.batch.common.job.CoroutineBatchJob
import server.batch.common.transaction.R2dbcTransactionExecutor

@Component
internal class DeleteEventOutboxTasklet(
    private val databaseClient: DatabaseClient,
    private val txExecutor: R2dbcTransactionExecutor,
) : CoroutineBatchJob {

    override val jobName: String = "deleteEventOutboxJob"

    override suspend fun run(parameters: Map<String, String>) {
        txExecutor.execute {
            databaseClient.sql("DELETE FROM event_outbox")
                .fetch()
                .rowsUpdated()
                .awaitSingle()
        }
    }
}

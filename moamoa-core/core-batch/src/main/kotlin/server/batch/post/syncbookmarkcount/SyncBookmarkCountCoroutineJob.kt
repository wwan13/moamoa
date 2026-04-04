package server.batch.post.syncbookmarkcount

import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Component
import server.batch.common.job.CoroutineBatchJob
import server.batch.common.transaction.R2dbcTransactionExecutor

@Component
internal class SyncBookmarkCountCoroutineJob(
    private val databaseClient: DatabaseClient,
    private val txExecutor: R2dbcTransactionExecutor,
) : CoroutineBatchJob {

    override val jobName: String = "syncBookmarkCountJob"

    override suspend fun run(parameters: Map<String, String>) {
        txExecutor.execute {
            databaseClient.sql(
                """
                UPDATE post p
                LEFT JOIN (
                    SELECT post_id, COUNT(*) AS cnt
                    FROM bookmark
                    GROUP BY post_id
                ) x ON x.post_id = p.id
                SET p.bookmark_count = COALESCE(x.cnt, 0)
                """.trimIndent()
            )
                .fetch()
                .rowsUpdated()
                .awaitSingle()
        }
    }
}

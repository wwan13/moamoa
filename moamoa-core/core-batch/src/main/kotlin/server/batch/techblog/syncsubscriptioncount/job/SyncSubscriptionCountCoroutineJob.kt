package server.batch.techblog.syncsubscriptioncount.job

import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Component
import server.batch.common.job.CoroutineBatchJob
import server.batch.common.transaction.R2dbcTransactionExecutor

@Component
internal class SyncSubscriptionCountCoroutineJob(
    private val databaseClient: DatabaseClient,
    private val txExecutor: R2dbcTransactionExecutor,
) : CoroutineBatchJob {

    override val jobName: String = "syncSubscriptionCountJob"

    override suspend fun run(parameters: Map<String, String>) {
        txExecutor.execute {
            databaseClient.sql(
                """
                UPDATE tech_blog tb
                LEFT JOIN (
                    SELECT tech_blog_id, COUNT(*) AS cnt
                    FROM subscription
                    GROUP BY tech_blog_id
                ) x ON x.tech_blog_id = tb.id
                SET tb.subscription_count = COALESCE(x.cnt, 0)
                """.trimIndent()
            )
                .fetch()
                .rowsUpdated()
                .awaitSingle()
        }
    }
}

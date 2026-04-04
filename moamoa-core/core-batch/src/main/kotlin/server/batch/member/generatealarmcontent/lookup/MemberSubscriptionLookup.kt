package server.batch.member.generatealarmcontent.lookup

import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Component

@Component
internal class MemberSubscriptionLookup(
    private val databaseClient: DatabaseClient
) {
    suspend fun load(): Map<Long, List<Long>> =
        databaseClient.sql(
            """
            SELECT
                tbs.member_id,
                tbs.tech_blog_id
            FROM subscription tbs
            JOIN tech_blog tb
              ON tb.id = tbs.tech_blog_id
            WHERE tbs.notification_enabled = 1
            """.trimIndent()
        )
            .fetch()
            .all()
            .collectList()
            .awaitSingle()
            .map { row ->
                (row["member_id"] as Number).toLong() to (row["tech_blog_id"] as Number).toLong()
            }
            .groupBy({ it.first }, { it.second })
}

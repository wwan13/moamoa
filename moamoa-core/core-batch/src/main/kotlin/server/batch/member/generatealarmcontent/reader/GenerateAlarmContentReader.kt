package server.batch.member.generatealarmcontent.reader

import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Component
import server.batch.member.generatealarmcontent.dto.MemberData

@Component
internal class GenerateAlarmContentReader(
    private val databaseClient: DatabaseClient,
) {

    suspend fun readAll(): List<MemberData> =
        databaseClient.sql(
            """
            SELECT m.id AS member_id, m.email AS email
            FROM member m
            """.trimIndent()
        )
            .fetch()
            .all()
            .collectList()
            .awaitSingle()
            .map { row ->
                MemberData(
                    memberId = (row["member_id"] as Number).toLong(),
                    email = row["email"] as String
                )
            }
}

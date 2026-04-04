package server.batch.techblog.collecttechblogpost.reader

import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Component
import server.batch.techblog.collecttechblogpost.dto.TechBlogKey

@Component
internal class FetchTechBlogPostReader(
    private val databaseClient: DatabaseClient,
) {

    suspend fun readAll(): List<TechBlogKey> =
        databaseClient.sql(
            """
            SELECT
              t.id,
              t.tech_blog_key,
              t.title
            FROM tech_blog t
            ORDER BY t.id ASC
            """.trimIndent()
        )
            .fetch()
            .all()
            .collectList()
            .awaitSingle()
            .map { row ->
                TechBlogKey(
                    id = (row["id"] as Number).toLong(),
                    techBlogKey = row["tech_blog_key"] as String,
                    title = row["title"] as String
                )
            }
}

package server.batch.member.generatealarmcontent.lookup

import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Component
import server.batch.member.generatealarmcontent.dto.PostData
import server.queue.QueueMemory
import server.queue.drain

@Component
internal class NewPostsLookup(
    private val queueMemory: QueueMemory,
    private val databaseClient: DatabaseClient
) {

    suspend fun load(): Map<Long, List<PostData>> {
        val postIds: List<Long> = queueMemory.drain("NEW_POST_IDS")
        queueMemory.delete("NEW_POST_IDS")

        if (postIds.isEmpty()) return emptyMap()

        val idClause = postIds.joinToString(",")
        return databaseClient.sql(
            """
        SELECT
            p.id            AS post_id,
            p.title         AS post_title,
            p.description   AS post_description,
            p.thumbnail     AS post_thumbnail,
            p.url           AS post_url,

            t.id            AS tech_blog_id,
            t.title         AS tech_blog_title,
            t.icon          AS tech_blog_icon
        FROM post p
        JOIN tech_blog t ON t.id = p.tech_blog_id
        WHERE p.id IN ($idClause)
        """.trimIndent()
        )
            .fetch()
            .all()
            .collectList()
            .awaitSingle()
            .map { row ->
                PostData(
                    postId = (row["post_id"] as Number).toLong(),
                    postTitle = row["post_title"] as String,
                    postDescription = row["post_description"] as String,
                    postThumbnail = row["post_thumbnail"] as String,
                    postUrl = row["post_url"] as String,
                    techBlogId = (row["tech_blog_id"] as Number).toLong(),
                    techBlogTitle = row["tech_blog_title"] as String,
                    techBlogIcon = row["tech_blog_icon"] as String
                )
            }
            .groupBy { it.techBlogId }
    }
}

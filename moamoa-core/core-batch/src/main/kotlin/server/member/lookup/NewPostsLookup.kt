package server.member.lookup

import kotlinx.coroutines.runBlocking
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import server.member.dto.PostData
import server.queue.QueueMemory

@Component
@StepScope
class NewPostsLookup(
    private val queueMemory: QueueMemory,
    private val jdbc: NamedParameterJdbcTemplate
) {

    val postsByTechBlogId: Map<Long, List<PostData>> by lazy {
        val postIds: List<Long> = runBlocking {
            val newPostIds = queueMemory.drain<Long>("NEW_POST_IDS")
            queueMemory.delete("NEW_POST_IDS")
            newPostIds
        }

        if (postIds.isEmpty()) return@lazy emptyMap()

        jdbc.query(
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
        WHERE p.id IN (:postIds)
        """.trimIndent(),
            mapOf("postIds" to postIds)
        ) { rs, _ ->
            PostData(
                postId = rs.getLong("post_id"),
                postTitle = rs.getString("post_title"),
                postDescription = rs.getString("post_description"),
                postThumbnail = rs.getString("post_thumbnail"),
                postUrl = rs.getString("post_url"),
                techBlogId = rs.getLong("tech_blog_id"),
                techBlogTitle = rs.getString("tech_blog_title"),
                techBlogIcon = rs.getString("tech_blog_icon")
            )
        }.groupBy { it.techBlogId }
    }
}
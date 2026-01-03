package server.techblog.writer

import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import server.QueueMemory
import server.common.time.dbNow
import server.common.transaction.AfterCommitExecutor
import server.techblog.dto.PostData
import java.sql.Timestamp
import java.time.LocalDateTime

@StepScope
@Component
class FetchTechBlogPostsWriter(
    private val jdbc: NamedParameterJdbcTemplate,
    private val afterCommitExecutor: AfterCommitExecutor,
    private val queueMemory: QueueMemory
) : ItemWriter<List<PostData>> {

    override fun write(chunk: Chunk<out List<PostData>>) {
        val posts = chunk.flatten()
        if (posts.isEmpty()) return

        val startedAt = jdbc.dbNow()

        val categoryIdByTitle = upsertCategories(posts)
        val postIdMap = upsertPostsAndLoadIds(posts)
        upsertPostCategories(posts, postIdMap, categoryIdByTitle)

        val windowEnd = jdbc.dbNow()
        val newlyInsertedPostIds = findNewlyInsertedPostIds(posts, startedAt, windowEnd)

        enqueueResultAfterCommit(newlyInsertedPostIds)
    }

    private fun upsertCategories(posts: List<PostData>): Map<String, Long> {
        val categories = posts.asSequence()
            .flatMap { it.categories.asSequence() }
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .distinct()
            .toList()

        if (categories.isNotEmpty()) {
            val params = categories.map { mapOf("title" to it) }
            jdbc.batchUpdate(
                """
                INSERT IGNORE INTO category (title, created_at, last_modified_at)
                VALUES (:title, NOW(), NOW())
                """.trimIndent(),
                params.toTypedArray()
            )
        }

        return if (categories.isEmpty()) emptyMap()
        else jdbc.query(
            """
            SELECT id, title
            FROM category
            WHERE title IN (:titles)
            """.trimIndent(),
            mapOf("titles" to categories)
        ) { rs, _ ->
            rs.getString("title") to rs.getLong("id")
        }.toMap()
    }

    private fun upsertPostsAndLoadIds(posts: List<PostData>): Map<Pair<Long, String>, Long> {
        val postParams = posts.map {
            mapOf(
                "postKey" to it.key,
                "title" to it.title,
                "description" to it.description,
                "thumbnail" to it.thumbnail,
                "url" to it.url,
                "publishedAt" to it.publishedAt,
                "techBlogId" to it.techBlogId
            )
        }

        jdbc.batchUpdate(
            """
            INSERT INTO post (
                post_key,
                title,
                description,
                thumbnail,
                url,
                published_at,
                tech_blog_id,
                created_at,
                last_modified_at
            )
            VALUES (
                :postKey,
                :title,
                :description,
                :thumbnail,
                :url,
                :publishedAt,
                :techBlogId,
                NOW(),
                NOW()
            )
            ON DUPLICATE KEY UPDATE
                title = VALUES(title),
                description = VALUES(description),
                thumbnail = VALUES(thumbnail),
                url = VALUES(url),
                last_modified_at = NOW()
            """.trimIndent(),
            postParams.toTypedArray()
        )

        val pairs = posts.asSequence().map { it.techBlogId to it.key }.distinct().toList()
        val techBlogIds = pairs.map { it.first }.distinct()
        val postKeys = pairs.map { it.second }.distinct()

        return jdbc.query(
            """
            SELECT id, tech_blog_id, post_key
            FROM post
            WHERE tech_blog_id IN (:techBlogIds)
              AND post_key IN (:postKeys)
            """.trimIndent(),
            mapOf(
                "techBlogIds" to techBlogIds,
                "postKeys" to postKeys
            )
        ) { rs, _ ->
            (rs.getLong("tech_blog_id") to rs.getString("post_key")) to rs.getLong("id")
        }.toMap()
    }

    private fun upsertPostCategories(
        posts: List<PostData>,
        postIdMap: Map<Pair<Long, String>, Long>,
        categoryIdByTitle: Map<String, Long>
    ) {
        val params = posts.flatMap { post ->
            val postId = postIdMap[post.techBlogId to post.key] ?: return@flatMap emptyList()

            post.categories
                .asSequence()
                .map { it.trim().lowercase() }
                .filter { it.isNotBlank() }
                .distinct()
                .mapNotNull { category ->
                    val categoryId = categoryIdByTitle[category] ?: return@mapNotNull null
                    mapOf("postId" to postId, "categoryId" to categoryId)
                }
                .toList()
        }

        if (params.isEmpty()) return

        jdbc.batchUpdate(
            """
            INSERT IGNORE INTO post_category (
                post_id,
                category_id,
                created_at,
                last_modified_at
            )
            VALUES (
                :postId,
                :categoryId,
                NOW(),
                NOW()
            )
            """.trimIndent(),
            params.toTypedArray()
        )
    }

    private fun findNewlyInsertedPostIds(
        posts: List<PostData>,
        startedAt: LocalDateTime,
        windowEnd: LocalDateTime
    ): Set<Long> {
        val techBlogIds = posts.map { it.techBlogId }.distinct()
        val postKeys = posts.map { it.key }.distinct()

        return jdbc.query(
            """
            SELECT id
            FROM post
            WHERE tech_blog_id IN (:techBlogIds)
              AND post_key IN (:postKeys)
              AND created_at >= :startedAt
              AND created_at <=  :windowEnd
            """.trimIndent(),
            mapOf(
                "techBlogIds" to techBlogIds,
                "postKeys" to postKeys,
                "startedAt" to Timestamp.valueOf(startedAt),
                "windowEnd" to Timestamp.valueOf(windowEnd),
            )
        ) { rs, _ -> rs.getLong("id") }
            .toSet()
    }

    private fun enqueueResultAfterCommit(newPostIds: Set<Long>) {
        if (newPostIds.isEmpty()) return

        afterCommitExecutor.execute {
            queueMemory.delete("NEW_POST_IDS")
            queueMemory.rPushAll("NEW_POST_IDS", newPostIds)
        }
    }
}
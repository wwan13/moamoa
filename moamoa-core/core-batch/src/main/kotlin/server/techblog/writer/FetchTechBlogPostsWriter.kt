package server.techblog.writer

import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import server.techblog.dto.PostData

@Component
class FetchTechBlogPostsWriter(
    private val jdbc: NamedParameterJdbcTemplate,
) : ItemWriter<List<PostData>> {

    override fun write(chunk: Chunk<out List<PostData>>) {
        val posts = chunk.flatten()
        if (posts.isEmpty()) return

        // category upsert
        val categories = posts.asSequence()
            .flatMap { it.categories.asSequence() }
            .map { it.trim() }
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


        val categoryIdByTitle: Map<String, Long> =
            if (categories.isEmpty()) emptyMap()
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

        // post upsert
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

        val postKeyPairs = posts.map {
            it.techBlogId to it.key
        }.distinct()

        // post category upsert
        val techBlogIds = postKeyPairs.map { it.first }.distinct()
        val postKeys = postKeyPairs.map { it.second }.distinct()

        val postIdMap =
            jdbc.query(
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

        val postCategoryParams = posts.flatMap { post ->
            val postId = postIdMap[post.techBlogId to post.key] ?: return@flatMap emptyList()

            post.categories
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .mapNotNull { category ->
                    val categoryId = categoryIdByTitle[category] ?: return@mapNotNull null
                    mapOf(
                        "postId" to postId,
                        "categoryId" to categoryId
                    )
                }
        }

        if (postCategoryParams.isNotEmpty()) {
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
                postCategoryParams.toTypedArray()
            )
        }
    }
}

package server.batch.techblog.collecttechblogpost.writer

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Component
import server.batch.techblog.collecttechblogpost.dto.PostData
import server.batch.techblog.collecttechblogpost.monitoring.TechBlogCollectMonitorStore
import server.cache.CacheMemory
import server.queue.QueueMemory
import java.time.LocalDateTime
import io.github.oshai.kotlinlogging.KotlinLogging.logger as kLogger

@Component
internal class PersistTechBlogPostWriter(
    private val databaseClient: DatabaseClient,
    private val queueMemory: QueueMemory,
    private val cacheMemory: CacheMemory,
    private val monitorStore: TechBlogCollectMonitorStore,
) {

    suspend fun write(posts: List<PostData>, runId: Long) {
        if (posts.isEmpty()) return

        val startedAt = dbNow()

        val tagIdByTitle = upsertTags(posts)
        val postIdMap = upsertPostsAndLoadIds(posts)
        upsertPostTags(posts, postIdMap, tagIdByTitle)

        val windowEnd = dbNow()
        val actualRunId = runId
        val newlyInsertedPostIds = findNewlyInsertedPostIds(posts, startedAt, windowEnd)
        val newlyInsertedCountsByTechBlogId = findNewlyInsertedCountsByTechBlogId(posts, startedAt, windowEnd)

        queueMemory.delete("NEW_POST_IDS")
        queueMemory.rPushAll("NEW_POST_IDS", newlyInsertedPostIds)
        cacheMemory.evictByPrefix("POST:LIST:")
        runCatching {
            monitorStore.accumulateAddedCount(actualRunId, newlyInsertedCountsByTechBlogId)
        }.onFailure {
            log.warn(
                "Failed to accumulate added counts. runId={}, techBlogCount={}",
                actualRunId,
                newlyInsertedCountsByTechBlogId.size,
                it
            )
        }
    }

    private suspend fun upsertTags(posts: List<PostData>): Map<String, Long> {
        val tags = posts.asSequence()
            .flatMap { it.tags.asSequence() }
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .distinct()
            .toList()

        if (tags.isEmpty()) return emptyMap()

        tags.forEach { tag ->
            databaseClient.sql(
                """
                INSERT IGNORE INTO tag (title, created_at, last_modified_at)
                VALUES (:title, NOW(), NOW())
                """.trimIndent(),
            )
                .bind("title", tag)
                .fetch()
                .rowsUpdated()
                .awaitSingle()
        }

        val result = mutableMapOf<String, Long>()
        tags.forEach { tag ->
            val row = databaseClient.sql(
                """
                SELECT id, title
                FROM tag
                WHERE title = :title
                LIMIT 1
                """.trimIndent()
            )
                .bind("title", tag)
                .fetch()
                .first()
                .awaitSingle()
            result[row["title"] as String] = (row["id"] as Number).toLong()
        }
        return result
    }

    private suspend fun upsertPostsAndLoadIds(posts: List<PostData>): Map<Pair<Long, String>, Long> {
        posts.forEach { post ->
            databaseClient.sql(
                """
                INSERT INTO post (
                    post_key,
                    title,
                    description,
                    thumbnail,
                    url,
                    published_at,
                    tech_blog_id,
                    category_id,
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
                    :categoryId,
                    NOW(),
                    NOW()
                )
                ON DUPLICATE KEY UPDATE
                    title = VALUES(title),
                    description = VALUES(description),
                    thumbnail = VALUES(thumbnail),
                    url = VALUES(url),
                    last_modified_at = NOW()
                """.trimIndent()
            )
                .bind("postKey", post.key)
                .bind("title", post.title)
                .bind("description", post.description)
                .bind("thumbnail", post.thumbnail)
                .bind("url", post.url)
                .bind("publishedAt", post.publishedAt)
                .bind("techBlogId", post.techBlogId)
                .bind("categoryId", post.categoryId)
                .fetch()
                .rowsUpdated()
                .awaitSingle()
        }

        val result = mutableMapOf<Pair<Long, String>, Long>()
        posts.asSequence().map { it.techBlogId to it.key }.distinct().forEach { (techBlogId, postKey) ->
            val row = databaseClient.sql(
                """
                SELECT id
                FROM post
                WHERE tech_blog_id = :techBlogId
                  AND post_key = :postKey
                LIMIT 1
                """.trimIndent()
            )
                .bind("techBlogId", techBlogId)
                .bind("postKey", postKey)
                .fetch()
                .first()
                .awaitSingle()
            result[techBlogId to postKey] = (row["id"] as Number).toLong()
        }
        return result
    }

    private suspend fun upsertPostTags(
        posts: List<PostData>,
        postIdMap: Map<Pair<Long, String>, Long>,
        tagIdByTitle: Map<String, Long>
    ) {
        posts.forEach { post ->
            val postId = postIdMap[post.techBlogId to post.key] ?: return@forEach
            post.tags
                .asSequence()
                .map { it.trim().lowercase() }
                .filter { it.isNotBlank() }
                .distinct()
                .forEach { tag ->
                    val tagId = tagIdByTitle[tag] ?: return@forEach
                    databaseClient.sql(
                        """
                        INSERT IGNORE INTO post_tag (
                            post_id,
                            tag_id,
                            created_at,
                            last_modified_at
                        )
                        VALUES (
                            :postId,
                            :tagId,
                            NOW(),
                            NOW()
                        )
                        """.trimIndent()
                    )
                        .bind("postId", postId)
                        .bind("tagId", tagId)
                        .fetch()
                        .rowsUpdated()
                        .awaitSingle()
                }
        }
    }

    private suspend fun findNewlyInsertedPostIds(
        posts: List<PostData>,
        startedAt: LocalDateTime,
        windowEnd: LocalDateTime
    ): Set<Long> {
        val result = mutableSetOf<Long>()
        posts.asSequence().map { it.techBlogId to it.key }.distinct().forEach { (techBlogId, postKey) ->
            val row = databaseClient.sql(
                """
                SELECT id
                FROM post
                WHERE tech_blog_id = :techBlogId
                  AND post_key = :postKey
                  AND created_at >= :startedAt
                  AND created_at <= :windowEnd
                LIMIT 1
                """.trimIndent()
            )
                .bind("techBlogId", techBlogId)
                .bind("postKey", postKey)
                .bind("startedAt", startedAt)
                .bind("windowEnd", windowEnd)
                .fetch()
                .first()
                .awaitSingleOrNull()
            if (row != null) {
                result += (row["id"] as Number).toLong()
            }
        }
        return result
    }

    private suspend fun findNewlyInsertedCountsByTechBlogId(
        posts: List<PostData>,
        startedAt: LocalDateTime,
        windowEnd: LocalDateTime
    ): Map<Long, Int> {
        val result = mutableMapOf<Long, Int>()
        posts.map { it.techBlogId }.distinct().forEach { techBlogId ->
            val row = databaseClient.sql(
                """
                SELECT COUNT(*) AS cnt
                FROM post
                WHERE tech_blog_id = :techBlogId
                  AND created_at >= :startedAt
                  AND created_at <= :windowEnd
                """.trimIndent()
            )
                .bind("techBlogId", techBlogId)
                .bind("startedAt", startedAt)
                .bind("windowEnd", windowEnd)
                .fetch()
                .first()
                .awaitSingle()
            result[techBlogId] = (row["cnt"] as Number).toInt()
        }
        return result
    }

    private suspend fun dbNow(): LocalDateTime {
        val row = databaseClient.sql("SELECT NOW() AS now")
            .fetch()
            .first()
            .awaitSingle()
        return row["now"] as LocalDateTime
    }

    companion object {
        private val log = kLogger {}
    }
}

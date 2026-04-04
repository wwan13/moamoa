package server.batch.post.updatepostviewcount.writer

import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Component
import server.batch.post.updatepostviewcount.dto.PostViewCount
import server.cache.CacheMemory
import server.set.SetMemory
import io.github.oshai.kotlinlogging.KotlinLogging.logger as kLogger

@Component
internal class UpdatePostViewCountWriter(
    private val databaseClient: DatabaseClient,
    private val cacheMemory: CacheMemory,
    private val setMemory: SetMemory,
) {

    private val dirtySetKey = "POST:VIEW_COUNT:DIRTY_SET"

    suspend fun write(items: List<PostViewCount>) {
        if (items.isEmpty()) return

        items.forEach { item ->
            databaseClient.sql(
                """
                UPDATE post
                SET view_count = view_count + :delta
                WHERE id = :postId
                """.trimIndent()
            )
                .bind("delta", item.delta)
                .bind("postId", item.postId)
                .fetch()
                .rowsUpdated()
                .awaitSingle()
        }

        items.forEach { item ->
            runCatching {
                val remaining = cacheMemory.decrBy(item.cacheKey, item.delta)
                if (remaining <= 0L) {
                    cacheMemory.evict(item.cacheKey)
                    setMemory.remove(dirtySetKey, item.postId.toString())
                }
            }.onFailure {
                log.warn(
                    "Failed to compensate post view count cache. cacheKey={}, delta={}",
                    item.cacheKey,
                    item.delta,
                    it
                )
            }
        }
    }

    companion object {
        private val log = kLogger {}
    }
}

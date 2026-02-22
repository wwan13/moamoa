package server.batch.post.writer

import io.github.oshai.kotlinlogging.KotlinLogging.logger as kLogger
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import server.batch.common.transaction.AfterCommitExecutor
import server.batch.post.dto.PostViewCount
import server.shared.cache.CacheMemory
import server.shared.set.SetMemory

@Component
internal class UpdatePostViewCountWriter(
    private val jdbc: NamedParameterJdbcTemplate,
    private val afterCommitExecutor: AfterCommitExecutor,
    private val cacheMemory: CacheMemory,
    private val setMemory: SetMemory,
) : ItemWriter<PostViewCount> {

    private val dirtySetKey = "POST:VIEW_COUNT:DIRTY_SET"

    override fun write(chunk: Chunk<out PostViewCount>) {
        val items = chunk.toList()
        if (items.isEmpty()) return

        jdbc.batchUpdate(
            """
            UPDATE post
            SET view_count = view_count + :delta
            WHERE id = :postId
            """.trimIndent(),
            items.map { item ->
                mapOf(
                    "postId" to item.postId,
                    "delta" to item.delta
                )
            }.toTypedArray()
        )

        afterCommitExecutor.execute {
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
    }

    companion object {
        private val log = kLogger {}
    }
}

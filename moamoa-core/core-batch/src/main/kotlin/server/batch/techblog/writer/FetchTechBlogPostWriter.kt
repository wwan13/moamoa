package server.batch.techblog.writer

import kotlinx.coroutines.runBlocking
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import server.batch.techblog.dto.PostData
import server.queue.QueueMemory

@StepScope
@Component
internal class FetchTechBlogPostWriter(
    private val queueMemory: QueueMemory,
    @field:Value("#{jobParameters['run.id']}") private val runId: Long?,
) : ItemWriter<List<PostData>> {

    private val queueKey = "TECH_BLOG:FETCHED_POSTS:${runId ?: "unknown"}"
    private var initialized = false

    override fun write(chunk: Chunk<out List<PostData>>) = runBlocking {
        if (!initialized) {
            queueMemory.delete(queueKey)
            initialized = true
        }

        val posts = chunk.flatMap { it }
        if (posts.isEmpty()) return@runBlocking

        queueMemory.rPushAll(queueKey, posts)
    }
}

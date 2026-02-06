package server.batch.techblog.reader

import kotlinx.coroutines.runBlocking
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.ItemReader
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import server.batch.techblog.dto.PostData
import server.queue.QueueMemory

@StepScope
@Component
internal class PersistTechBlogPostReader(
    private val queueMemory: QueueMemory,
    @field:Value("#{jobParameters['run.id']}") private val runId: Long?,
) : ItemReader<List<PostData>> {

    private val queueKey = "TECH_BLOG:FETCHED_POSTS:${runId ?: "unknown"}"

    override fun read(): List<PostData>? = runBlocking {
        val items = queueMemory.drain<PostData>(queueKey, max = 200)
        if (items.isEmpty()) return@runBlocking null
        items
    }
}

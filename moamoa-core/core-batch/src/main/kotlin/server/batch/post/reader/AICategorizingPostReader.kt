package server.batch.post.reader

import kotlinx.coroutines.runBlocking
import org.springframework.batch.item.ItemReader
import org.springframework.stereotype.Component
import server.batch.post.dto.PostSummary
import server.queue.QueueMemory

@Component
internal class AICategorizingPostReader(
    private val queueMemory: QueueMemory,
) : ItemReader<List<PostSummary>> {

    override fun read(): List<PostSummary>? = runBlocking {
        val items = queueMemory.drain<PostSummary>("AI_CATEGORIZING_POSTS", max = 20)
        if (items.isEmpty()) return@runBlocking null
        items
    }
}

package server.batch.post.updatepostviewcount.job

import org.springframework.stereotype.Component
import server.batch.common.job.CoroutineBatchJob
import server.batch.post.updatepostviewcount.reader.UpdatePostViewCountReader
import server.batch.post.updatepostviewcount.writer.UpdatePostViewCountWriter

@Component
internal class UpdatePostViewCountCoroutineJob(
    private val reader: UpdatePostViewCountReader,
    private val writer: UpdatePostViewCountWriter,
) : CoroutineBatchJob {

    override val jobName: String = "updatePostViewCountJob"

    override suspend fun run(parameters: Map<String, String>) {
        val items = reader.loadItems()
        writer.write(items)
    }
}

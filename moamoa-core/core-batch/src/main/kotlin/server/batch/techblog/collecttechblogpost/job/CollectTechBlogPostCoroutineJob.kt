package server.batch.techblog.collecttechblogpost.job

import org.springframework.stereotype.Component
import server.batch.common.job.CoroutineBatchJob
import server.batch.techblog.collecttechblogpost.processor.FetchTechBlogPostProcessor
import server.batch.techblog.collecttechblogpost.reader.FetchTechBlogPostReader
import server.batch.techblog.collecttechblogpost.writer.PersistTechBlogPostWriter

@Component
internal class CollectTechBlogPostCoroutineJob(
    private val reader: FetchTechBlogPostReader,
    private val processor: FetchTechBlogPostProcessor,
    private val writer: PersistTechBlogPostWriter,
) : CoroutineBatchJob {

    override val jobName: String = "collectTechBlogPostJob"

    override suspend fun run(parameters: Map<String, String>) {
        val runId = parameters["run.id"]?.toLongOrNull() ?: System.currentTimeMillis()
        val postLimit = parameters["postLimit"]?.toIntOrNull()

        val techBlogs = reader.readAll()
        if (techBlogs.isEmpty()) return

        val posts = mutableListOf<server.batch.techblog.collecttechblogpost.dto.PostData>()
        techBlogs.forEach { techBlog ->
            posts += processor.process(techBlog, runId = runId, postLimit = postLimit)
        }
        writer.write(posts, runId)
    }
}

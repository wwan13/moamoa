package server.batch.techblog.processor

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.ItemProcessor
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import server.batch.techblog.dto.PostData
import server.batch.techblog.dto.TechBlogKey
import server.batch.techblog.monitoring.TechBlogCollectMonitorStore
import server.techblog.TechBlogSources

@StepScope
@Component
internal class FetchTechBlogPostProcessor(
    private val techBlogSources: TechBlogSources,
    private val monitorStore: TechBlogCollectMonitorStore,
    @field:Value("#{jobParameters['run.id']}") private val runId: Long?,
    @field:Value("#{jobParameters['postLimit']}") private val postLimit: Long?,
) : ItemProcessor<TechBlogKey, List<PostData>> {

    override fun process(item: TechBlogKey): List<PostData>? = runBlocking {
        val actualRunId = runId ?: System.currentTimeMillis()

        runCatching {
            val source = techBlogSources[item.techBlogKey]
            source.getPosts(postLimit?.toInt())
                .map {
                    PostData(
                        key = it.key,
                        title = it.title,
                        description = it.description,
                        tags = it.tags,
                        thumbnail = it.thumbnail,
                        publishedAt = it.publishedAt,
                        url = it.url,
                        techBlogId = item.id
                    )
                }
                .toList()
        }.fold(
            onSuccess = { posts ->
                runCatching {
                    monitorStore.recordFetchSuccess(
                        runId = actualRunId,
                        techBlog = item,
                        fetchedPostCount = posts.size
                    )
                }.onFailure {
                    log.warn(
                        "Failed to record tech blog fetch success. techBlogId={}, runId={}",
                        item.id,
                        actualRunId,
                        it
                    )
                }
                posts
            },
            onFailure = { throwable ->
                runCatching {
                    monitorStore.recordFetchFailure(
                        runId = actualRunId,
                        techBlog = item,
                        throwable = throwable
                    )
                }.onFailure {
                    log.warn(
                        "Failed to record tech blog fetch failure. techBlogId={}, runId={}",
                        item.id,
                        actualRunId,
                        it
                    )
                }
                emptyList()
            }
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(FetchTechBlogPostProcessor::class.java)
    }
}

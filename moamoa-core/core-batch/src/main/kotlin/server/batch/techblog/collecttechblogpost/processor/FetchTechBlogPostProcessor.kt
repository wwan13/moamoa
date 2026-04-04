package server.batch.techblog.collecttechblogpost.processor

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import io.github.oshai.kotlinlogging.KotlinLogging.logger as kLogger
import org.springframework.stereotype.Component
import server.batch.techblog.collecttechblogpost.dto.PostData
import server.batch.techblog.collecttechblogpost.dto.TechBlogKey
import server.batch.techblog.collecttechblogpost.monitoring.TechBlogCollectMonitorStore
import server.techblog.TechBlogPostCategory
import server.techblog.TechBlogPostCatetorizer
import server.techblog.TechBlogSources

@Component
internal class FetchTechBlogPostProcessor(
    private val techBlogSources: TechBlogSources,
    private val techBlogCategorizer: TechBlogPostCatetorizer,
    private val monitorStore: TechBlogCollectMonitorStore,
) {

    suspend fun process(
        item: TechBlogKey,
        runId: Long,
        postLimit: Int?,
    ): List<PostData> {
        val actualRunId = runId

        return runCatching {
            val source = techBlogSources[item.techBlogKey]
            source.getPosts(postLimit)
                .map {
                    val categorized = techBlogCategorizer.categorize(it)
                    PostData(
                        key = categorized.key,
                        title = categorized.title,
                        description = categorized.description,
                        tags = categorized.tags,
                        thumbnail = categorized.thumbnail,
                        publishedAt = categorized.publishedAt,
                        url = categorized.url,
                        categoryId = categorized.category.categoryId,
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
        private val log = kLogger {}
    }
}

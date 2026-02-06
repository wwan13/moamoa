package server.batch.techblog.processor

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.ItemProcessor
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import server.batch.techblog.dto.PostData
import server.batch.techblog.dto.TechBlogKey
import server.techblog.TechBlogSources

@StepScope
@Component
internal class FetchTechBlogPostProcessor(
    private val techBlogSources: TechBlogSources,
    @field:Value("#{jobParameters['postLimit']}") private val postLimit: Long?,
) : ItemProcessor<TechBlogKey, List<PostData>> {

    override fun process(item: TechBlogKey): List<PostData>? = runBlocking {
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
    }
}

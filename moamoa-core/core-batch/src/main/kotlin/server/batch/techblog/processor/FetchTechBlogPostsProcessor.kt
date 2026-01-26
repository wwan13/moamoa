package server.batch.techblog.processor

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component
import server.batch.techblog.dto.PostData
import server.batch.techblog.dto.TechBlogKey
import server.techblog.TechBlogSources

@Component
internal class FetchTechBlogPostsProcessor(
    private val techBlogSources: TechBlogSources,
) : ItemProcessor<TechBlogKey, List<PostData>> {

    override fun process(item: TechBlogKey): List<PostData>? = runBlocking {
        val source = techBlogSources[item.techBlogKey]

        source.getPosts(10)
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
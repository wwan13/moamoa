package server.techblog.processor

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component
import server.techblog.TechBlogSources
import server.techblog.dto.PostData
import server.techblog.dto.TechBlogKey

@Component
class FetchTechBlogPostsProcessor(
    private val techBlogSources: TechBlogSources,
) : ItemProcessor<TechBlogKey, List<PostData>> {

    override fun process(item: TechBlogKey): List<PostData>? = runBlocking {
        val source = techBlogSources[item.techBlogKey]

        source.getPosts(20)
            .map {
                PostData(
                    key = it.key,
                    title = it.title,
                    description = it.description,
                    categories = it.categories,
                    thumbnail = it.thumbnail,
                    publishedAt = it.publishedAt,
                    url = it.url,
                    techBlogId = item.id
                )
            }
            .toList()
    }
}
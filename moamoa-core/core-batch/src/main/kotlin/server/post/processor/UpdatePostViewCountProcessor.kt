package server.post.processor

import kotlinx.coroutines.runBlocking
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component
import server.CacheMemory
import server.post.dto.PostViewCount

@Component
class UpdatePostViewCountProcessor(
    private val cacheMemory: CacheMemory
) : ItemProcessor<PostViewCount, PostViewCount> {

    private val postViewCountCachePrefix = "POST:VIEW_COUNT:"

    override fun process(item: PostViewCount): PostViewCount? = runBlocking {
        val cacheKey = cacheKey(item.postId)
        val increased = cacheMemory.get<Long>(cacheKey)
            ?: return@runBlocking null

        cacheMemory.evict(cacheKey)

        PostViewCount(
            item.postId,
            item.viewCount + increased
        )
    }

    private fun cacheKey(postId: Long) = postViewCountCachePrefix + postId
}
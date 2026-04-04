package server.batch.post.updatepostviewcount.reader

import org.springframework.stereotype.Component
import server.batch.post.updatepostviewcount.dto.PostViewCount
import server.cache.CacheMemory
import server.set.SetMemory

@Component
internal class UpdatePostViewCountReader(
    private val setMemory: SetMemory,
    private val cacheMemory: CacheMemory,
) {
    private val dirtySetKey = "POST:VIEW_COUNT:DIRTY_SET"
    private val postViewCountPrefix = "POST:VIEW_COUNT:"

    suspend fun loadItems(): List<PostViewCount> {
        val postIds = setMemory.members(dirtySetKey)
            .mapNotNull { it.toLongOrNull() }
            .sorted()

        if (postIds.isEmpty()) return emptyList()

        val keyByPostId = postIds.associateWith { postId -> cacheKey(postId) }
        val valuesByKey = cacheMemory.mget(keyByPostId.values)

        return postIds.mapNotNull { postId ->
            val cacheKey = keyByPostId.getValue(postId)
            val delta = valuesByKey[cacheKey]?.toLongOrNull() ?: return@mapNotNull null
            if (delta <= 0L) return@mapNotNull null

            PostViewCount(
                postId = postId,
                delta = delta,
                cacheKey = cacheKey
            )
        }
    }

    private fun cacheKey(postId: Long) = postViewCountPrefix + postId
}

package server.infra.cache

import org.springframework.stereotype.Component
import server.cache.CacheMemory
import server.set.SetMemory

@Component
class PostViewCountCache(
    private val cacheMemory: CacheMemory,
    private val setMemory: SetMemory,
) {

    private val postViewCountPrefix = "POST:VIEW_COUNT:"
    private val dirtySetKey = "POST:VIEW_COUNT:DIRTY_SET"

    private fun postViewCountKey(postId: Long) = postViewCountPrefix + postId

    suspend fun incr(postId: Long) {
        cacheMemory.incr(postViewCountKey(postId))
        setMemory.add(dirtySetKey, postId.toString())
    }
}

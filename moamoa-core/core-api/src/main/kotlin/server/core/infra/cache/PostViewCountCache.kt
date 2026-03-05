package server.core.infra.cache

import org.springframework.stereotype.Component
import server.cache.CacheMemory
import server.set.SetMemory

@Component
class PostViewCountCache(
    private val cacheMemory: server.cache.CacheMemory,
    private val setMemory: server.set.SetMemory,
) {

    private val postViewCountPrefix = "POST:VIEW_COUNT:"
    private val dirtySetKey = "POST:VIEW_COUNT:DIRTY_SET"

    private fun postViewCountKey(postId: Long) = postViewCountPrefix + postId

    fun incr(postId: Long) {
        cacheMemory.incr(postViewCountKey(postId))
        setMemory.add(dirtySetKey, postId.toString())
    }
}

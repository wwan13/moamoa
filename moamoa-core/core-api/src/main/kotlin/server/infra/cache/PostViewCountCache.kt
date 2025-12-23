package server.infra.cache

import org.springframework.stereotype.Component
import server.CacheMemory

@Component
class PostViewCountCache(
    private val cacheMemory: CacheMemory
) {

    private val postViewCountPrefix = "POST:VIEW_COUNT:"

    private fun postViewCountKey(postId: Long) = postViewCountPrefix + postId

    suspend fun incr(postId: Long) {
        cacheMemory.incr(postViewCountKey(postId))
    }
}
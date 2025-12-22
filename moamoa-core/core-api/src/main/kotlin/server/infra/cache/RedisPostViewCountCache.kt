package server.infra.cache

import org.springframework.stereotype.Component
import server.application.cache.PostViewCountCache

@Component
class RedisPostViewCountCache(
    private val cacheMemory: CacheMemory
) : PostViewCountCache {

    private val postViewCountPrefix = "POST:VIEW_COUNT:"

    private fun postViewCountKey(postId: Long) = postViewCountPrefix + postId

    override suspend fun incr(postId: Long) {
        cacheMemory.incr(postViewCountKey(postId))
    }
}
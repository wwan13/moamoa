package server.feature.post.command.application

import org.springframework.stereotype.Service
import server.infra.cache.PostViewCountCache

@Service
class PostService(
    private val postViewCountCache: PostViewCountCache
) {

    suspend fun increaseViewCount(postId: Long): IncreaseViewCountResult {
        postViewCountCache.incr(postId)
        return IncreaseViewCountResult(true)
    }
}
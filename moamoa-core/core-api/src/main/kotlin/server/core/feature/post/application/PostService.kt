package server.core.feature.post.application

import org.springframework.stereotype.Service
import server.core.feature.post.infra.PostViewCountCache

@Service
class PostService(
    private val postViewCountCache: PostViewCountCache
) {

    fun increaseViewCount(postId: Long): IncreaseViewCountResult {
        postViewCountCache.incr(postId)
        return IncreaseViewCountResult(true)
    }
}
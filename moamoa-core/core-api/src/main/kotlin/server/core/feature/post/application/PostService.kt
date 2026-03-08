package server.core.feature.post.application

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import server.core.feature.post.infra.PostViewCountCache

@Service
@Transactional
class PostService(
    private val postViewCountCache: PostViewCountCache
) {
    @Transactional(readOnly = true)
    fun increaseViewCount(postId: Long): IncreaseViewCountResult {
        postViewCountCache.incr(postId)
        return IncreaseViewCountResult(true)
    }
}

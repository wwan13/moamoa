package server.application

import org.springframework.stereotype.Service
import server.application.cache.PostViewCountCache
import server.domain.post.PostRepository

@Service
class PostService(
    private val postRepository: PostRepository,
    private val postViewCountCache: PostViewCountCache
) {

    suspend fun redirect(postId: Long): String {
        val post = postRepository.findById(postId)
            ?: throw IllegalArgumentException("존재하지 않는 블로그 개시글 입니다.")

        postViewCountCache.incr(postId)

        return post.url
    }
}
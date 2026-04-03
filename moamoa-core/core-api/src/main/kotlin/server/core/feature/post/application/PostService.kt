package server.core.feature.post.application

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import server.core.feature.post.domain.PostRepository
import server.core.feature.post.infra.PostViewCountCache

@Service
@Transactional
class PostService(
    private val postRepository: PostRepository,
    private val postViewCountCache: PostViewCountCache
) {
    @Transactional(readOnly = true)
    fun findById(postId: Long): PostData {
        val post = postRepository.findByIdOrNull(postId)
            ?: throw NoSuchElementException("존재하지 않는 게시글 입니다.")
        postViewCountCache.incr(postId)

        return PostData(post)
    }
}

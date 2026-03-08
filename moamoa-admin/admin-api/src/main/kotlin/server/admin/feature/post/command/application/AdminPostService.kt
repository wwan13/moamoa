package server.admin.feature.post.command.application

import org.springframework.stereotype.Service
import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.annotation.Transactional
import server.admin.feature.post.command.domain.AdminPostRepository

@Service
internal class AdminPostService(
    private val postRepository: AdminPostRepository,
) {

    @Transactional
    fun updateCategory(
        postId: Long,
        command: AdminUpdateCategoryCommand
    ): AdminUpdateCategoryResult {
        val post = postRepository.findByIdOrNull(postId)
            ?: throw IllegalArgumentException("존재하지 않는 게시글 입니다.")

        post.updateCategory(command.categoryId)

        return AdminUpdateCategoryResult(true)
    }
}

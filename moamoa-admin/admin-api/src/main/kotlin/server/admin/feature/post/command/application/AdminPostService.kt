package server.admin.feature.post.command.application

import org.springframework.stereotype.Service
import org.springframework.data.repository.findByIdOrNull
import server.admin.feature.post.command.domain.AdminPostRepository
import server.admin.infra.db.transaction.AdminTransactional

@Service
internal class AdminPostService(
    private val transactional: AdminTransactional,
    private val postRepository: AdminPostRepository,
) {

    fun updateCategory(
        postId: Long,
        command: AdminUpdateCategoryCommand
    ) = transactional {
        val post = postRepository.findByIdOrNull(postId)
            ?: throw IllegalArgumentException("존재하지 않는 게시글 입니다.")

        post.updateCategory(command.categoryId)

        AdminUpdateCategoryResult(true)
    }
}

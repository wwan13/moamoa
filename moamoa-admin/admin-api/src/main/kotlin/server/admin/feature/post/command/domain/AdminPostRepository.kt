package server.admin.feature.post.command.domain

import org.springframework.data.repository.kotlin.CoroutineCrudRepository

internal interface AdminPostRepository : CoroutineCrudRepository<AdminPost, Long> {
    suspend fun existsByTechBlogId(techBlogId: Long): Boolean
}

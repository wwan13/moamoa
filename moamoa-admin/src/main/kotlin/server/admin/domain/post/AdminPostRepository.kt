package server.admin.domain.post

import org.springframework.data.repository.kotlin.CoroutineCrudRepository

internal interface AdminPostRepository : CoroutineCrudRepository<AdminPost, Long> {
    suspend fun existsByTechBlogId(techBlogId: Long): Boolean
}
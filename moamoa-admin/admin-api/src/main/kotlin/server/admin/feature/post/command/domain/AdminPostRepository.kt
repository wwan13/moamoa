package server.admin.feature.post.command.domain

import org.springframework.data.jpa.repository.JpaRepository

internal interface AdminPostRepository : JpaRepository<AdminPost, Long> {
    fun existsByTechBlogId(techBlogId: Long): Boolean
}

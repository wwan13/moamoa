package server.admin.domain.post

import org.springframework.data.jpa.repository.JpaRepository

interface AdminPostRepository : JpaRepository<AdminPost, Long> {
    fun existsByTechBlogId(techBlogId: Long): Boolean
}
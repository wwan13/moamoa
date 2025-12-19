package server.admin.domain.techblog

import org.springframework.data.jpa.repository.JpaRepository

interface AdminTechBlogRepository : JpaRepository<AdminTechBlog, Long> {
    fun existsByTitle(title: String): Boolean
}
package server.admin.feature.techblog.domain

import org.springframework.data.jpa.repository.JpaRepository

internal interface AdminTechBlogRepository : JpaRepository<AdminTechBlog, Long> {
    fun existsByTitle(title: String): Boolean
}

package server.admin.domain.techblog

import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface AdminTechBlogRepository : CoroutineCrudRepository<AdminTechBlog, Long> {
    suspend fun existsByTitle(title: String): Boolean
}
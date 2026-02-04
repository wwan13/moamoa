package server.admin.feature.techblog.domain

import org.springframework.data.repository.kotlin.CoroutineCrudRepository

internal interface AdminTechBlogRepository : CoroutineCrudRepository<AdminTechBlog, Long> {
    suspend fun existsByTitle(title: String): Boolean
}

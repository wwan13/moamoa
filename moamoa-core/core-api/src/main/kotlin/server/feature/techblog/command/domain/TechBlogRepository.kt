package server.feature.techblog.command.domain

import org.springframework.data.jpa.repository.JpaRepository

interface TechBlogRepository : JpaRepository<TechBlog, Long> {
    fun findByKey(key: String): TechBlog?

    fun findAllByOrderByTitleAsc(): List<TechBlog>
}

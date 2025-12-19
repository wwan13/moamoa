package server.domain.techblog

import org.springframework.data.jpa.repository.JpaRepository

interface TechBlogRepository : JpaRepository<TechBlog, Long> {
}
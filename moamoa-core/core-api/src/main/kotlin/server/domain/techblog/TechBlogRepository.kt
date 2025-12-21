package server.domain.techblog

import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface TechBlogRepository : CoroutineCrudRepository<TechBlog, Long> {
}
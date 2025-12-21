package server.domain.postcategory

import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface PostCategoryRepository : CoroutineCrudRepository<PostCategory, Long> {
}
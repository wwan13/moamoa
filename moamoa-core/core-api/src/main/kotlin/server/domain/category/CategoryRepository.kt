package server.domain.category

import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface CategoryRepository : CoroutineCrudRepository<Category, Long> {
    suspend fun findAllByOrderByTitleAsc(): List<Category>
}
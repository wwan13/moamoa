package server.admin.domain.category

import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface AdminCategoryRepository : CoroutineCrudRepository<AdminCategory, Long> {
    suspend fun findAllByTitleIn(titles: List<String>): List<AdminCategory>
}
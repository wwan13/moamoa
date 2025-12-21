package server.application

import org.springframework.stereotype.Service
import server.domain.category.CategoryRepository

@Service
class CategoryService(
    private val categoryRepository: CategoryRepository
) {

    suspend fun findAll(): List<CategoryData> {
        return categoryRepository.findAllByOrderByTitleAsc().map(::CategoryData)
    }
}
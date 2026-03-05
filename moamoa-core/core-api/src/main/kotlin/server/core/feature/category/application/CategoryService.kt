package server.core.feature.category.application

import org.springframework.stereotype.Service
import server.core.feature.category.domain.Category

@Service
class CategoryService {

    fun findAll(): List<CategoryData> {
        return Category.Companion.validCategories.map(::CategoryData)
    }
}
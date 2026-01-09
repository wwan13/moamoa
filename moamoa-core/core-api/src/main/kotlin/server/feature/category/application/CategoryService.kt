package server.feature.category.application

import org.springframework.stereotype.Service
import server.feature.category.domain.Category

@Service
class CategoryService {

    fun findAll(): List<CategoryData> {
        return Category.validCategories.map(::CategoryData)
    }
}
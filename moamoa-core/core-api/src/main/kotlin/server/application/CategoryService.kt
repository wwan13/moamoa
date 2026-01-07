package server.application

import org.springframework.stereotype.Service
import server.domain.category.Category

@Service
class CategoryService {

    fun findAll(): List<CategoryData> {
        return Category.validCategories.map(::CategoryData)
    }
}
package server.core.feature.category.application

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import server.core.feature.category.domain.Category

@Service
@Transactional
class CategoryService {
    @Transactional(readOnly = true)
    fun findAll(): List<CategoryData> {
        return Category.Companion.validCategories.map(::CategoryData)
    }
}

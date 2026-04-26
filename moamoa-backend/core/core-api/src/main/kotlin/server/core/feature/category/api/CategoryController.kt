package server.core.feature.category.api

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import server.core.feature.category.application.CategoryData
import server.core.feature.category.application.CategoryService
import server.core.global.web.ApiResponse

@RestController
@RequestMapping("/api/category")
class CategoryController(
    private val categoryService: CategoryService
) {

    @GetMapping
    fun findAll(): ApiResponse<List<CategoryData>> {
        val response = categoryService.findAll()

        return ApiResponse.of(response)
    }
}

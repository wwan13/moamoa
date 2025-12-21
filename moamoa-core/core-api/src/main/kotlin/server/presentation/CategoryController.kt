package server.presentation

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import server.application.CategoryData
import server.application.CategoryService

@RestController
@RequestMapping("/api/category")
class CategoryController(
    private val categoryService: CategoryService
) {
    @GetMapping
    suspend fun findAll(): ResponseEntity<List<CategoryData>> {
        val response = categoryService.findAll()
        return ResponseEntity.ok(response)
    }
}
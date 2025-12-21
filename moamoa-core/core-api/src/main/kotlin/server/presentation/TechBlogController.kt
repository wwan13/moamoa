package server.presentation

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import server.application.TechBlogData
import server.application.TechBlogService

@RestController
@RequestMapping("/api/tech-blog")
class TechBlogController(
    private val techBlogService: TechBlogService
) {

    @GetMapping("/{techBlogId}")
    suspend fun findById(
        @PathVariable techBlogId: Long
    ): ResponseEntity<TechBlogData> {
        val response = techBlogService.findById(techBlogId)
        return ResponseEntity.ok(response)
    }

    @GetMapping
    suspend fun findAll(): ResponseEntity<List<TechBlogData>> {
        val response = techBlogService.findAll()
        return ResponseEntity.ok(response)
    }
}
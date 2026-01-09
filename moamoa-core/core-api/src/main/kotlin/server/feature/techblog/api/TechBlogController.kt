package server.feature.techblog.api

import kotlinx.coroutines.flow.toList
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import server.feature.techblog.application.TechBlogData
import server.feature.techblog.application.TechBlogService

@RestController
@RequestMapping("/api/tech-blog")
class TechBlogController(
    private val techBlogService: TechBlogService
) {

    @GetMapping("/{techBlogKey}")
    suspend fun findById(
        @PathVariable techBlogKey: String
    ): ResponseEntity<TechBlogData> {
        val response = techBlogService.findByKey(techBlogKey)
        return ResponseEntity.ok(response)
    }

    @GetMapping
    suspend fun findAll(): ResponseEntity<List<TechBlogData>> {
        val response = techBlogService.findAll().toList()
        return ResponseEntity.ok(response)
    }
}
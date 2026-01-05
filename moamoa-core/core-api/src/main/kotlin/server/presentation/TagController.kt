package server.presentation

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import server.application.TagData
import server.application.TagService

@RestController
@RequestMapping("/api/tag")
class TagController(
    private val tagService: TagService
) {
    @GetMapping
    suspend fun findAll(): ResponseEntity<List<TagData>> {
        val response = tagService.findAll()
        return ResponseEntity.ok(response)
    }
}
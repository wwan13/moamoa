package server.feature.tag.api

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import server.feature.tag.application.TagData
import server.feature.tag.application.TagService

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
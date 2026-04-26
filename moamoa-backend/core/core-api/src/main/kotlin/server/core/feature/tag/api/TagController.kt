package server.core.feature.tag.api

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import server.core.feature.tag.application.TagData
import server.core.feature.tag.application.TagService
import server.core.global.web.ApiResponse

@RestController
@RequestMapping("/api/tag")
class TagController(
    private val tagService: TagService
) {
    @GetMapping
    fun findAll(): ApiResponse<List<TagData>> {
        val response = tagService.findAll()
        return ApiResponse.of(response)
    }
}

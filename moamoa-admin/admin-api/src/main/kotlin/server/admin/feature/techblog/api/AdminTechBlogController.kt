package server.admin.feature.techblog.api

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import support.admin.uri.toUri

@RestController
@RequestMapping("/api/admin/tech-blog")
internal class AdminTechBlogController(
    private val techBlogService: server.admin.feature.techblog.application.AdminTechBlogService
) {
    @PostMapping
    suspend fun create(
        @RequestBody command: server.admin.feature.techblog.application.AdminCreateTechBlogCommand
    ): ResponseEntity<server.admin.feature.techblog.application.AdminTechBlogData> {
        val response = techBlogService.create(command)
        val uri = "/api/tech-blog/${response.id}".toUri()
        return ResponseEntity.created(uri).body(response)
    }

    @PatchMapping("/{techBlogId}")
    suspend fun update(
        @PathVariable techBlogId: Long,
        @RequestBody @Valid command: server.admin.feature.techblog.application.AdminUpdateTechBlogCommand
    ): ResponseEntity<server.admin.feature.techblog.application.AdminTechBlogData> {
        val response = techBlogService.update(techBlogId, command)
        return ResponseEntity.ok(response)
    }
}

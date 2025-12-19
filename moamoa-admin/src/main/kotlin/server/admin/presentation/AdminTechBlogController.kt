package server.admin.presentation

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import server.admin.application.AdminTechBlogService
import server.admin.application.AdminCreateTechBlogCommand
import server.admin.application.AdminTechBlogData
import server.admin.application.AdminUpdateTechBlogCommand
import support.admin.uri.toUri

@RestController
@RequestMapping("/api/admin/tech-blog")
class AdminTechBlogController(
    private val techBlogService: AdminTechBlogService
) {
    @PostMapping
    fun create(
        @RequestBody command: AdminCreateTechBlogCommand
    ): ResponseEntity<AdminTechBlogData> {
        val response = techBlogService.create(command)
        val uri = "/api/tech-blog/${response.id}".toUri()
        return ResponseEntity.created(uri).body(response)
    }

    @PatchMapping("/{techBlogId}")
    fun update(
        @PathVariable techBlogId: Long,
        @RequestBody command: AdminUpdateTechBlogCommand
    ): ResponseEntity<AdminTechBlogData> {
        val response = techBlogService.update(techBlogId, command)
        return ResponseEntity.ok(response)
    }
}
package server.admin.feature.techblog.api

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import server.admin.feature.techblog.application.*

@RestController
@RequestMapping("/api/admin/tech-blog")
internal class AdminTechBlogController(
    private val techBlogService: AdminTechBlogService
) {
    @PostMapping
    fun create(
        @RequestBody @Valid command: AdminCreateTechBlogCommand
    ): ResponseEntity<AdminTechBlogData> {
        val response = techBlogService.create(command)

        return ResponseEntity.ok(response)
    }

    @PostMapping("/init")
    fun init(
        @RequestBody @Valid command: AdminInitTechBlogCommand
    ): ResponseEntity<AdminInitTechBlogResult> {
        val response = techBlogService.initTechBlog(command)
        return ResponseEntity.ok(response)
    }

    @PatchMapping("/{techBlogId}")
    fun update(
        @PathVariable techBlogId: Long,
        @RequestBody @Valid command: AdminUpdateTechBlogCommand
    ): ResponseEntity<AdminTechBlogData> {
        val response = techBlogService.update(techBlogId, command)
        return ResponseEntity.ok(response)
    }
}

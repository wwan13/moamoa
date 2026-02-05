package server.admin.feature.techblog.api

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import server.admin.feature.techblog.application.AdminCreateTechBlogCommand
import server.admin.feature.techblog.application.AdminInitTechBlogCommand
import server.admin.feature.techblog.application.AdminInitTechBlogResult
import server.admin.feature.techblog.application.AdminTechBlogData
import server.admin.feature.techblog.application.AdminTechBlogService
import server.admin.feature.techblog.application.AdminUpdateTechBlogCommand
import support.admin.uri.toUri

@RestController
@RequestMapping("/api/admin/tech-blog")
internal class AdminTechBlogController(
    private val techBlogService: AdminTechBlogService
) {
    @PostMapping
    suspend fun create(
        @RequestBody @Valid command: AdminCreateTechBlogCommand
    ): ResponseEntity<AdminTechBlogData> {
        val response = techBlogService.create(command)
        val uri = "/api/tech-blog/${response.id}".toUri()
        return ResponseEntity.created(uri).body(response)
    }

    @PostMapping("/init")
    suspend fun init(
        @RequestBody @Valid command: AdminInitTechBlogCommand
    ): ResponseEntity<AdminInitTechBlogResult> {
        val response = techBlogService.initTechBlog(command)
        return ResponseEntity.ok(response)
    }

    @PatchMapping("/{techBlogId}")
    suspend fun update(
        @PathVariable techBlogId: Long,
        @RequestBody @Valid command: AdminUpdateTechBlogCommand
    ): ResponseEntity<AdminTechBlogData> {
        val response = techBlogService.update(techBlogId, command)
        return ResponseEntity.ok(response)
    }
}

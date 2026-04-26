package server.admin.feature.techblog.api

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*
import server.admin.feature.techblog.application.*
import server.admin.global.web.AdminApiResponse

@RestController
@RequestMapping("/api/admin/tech-blog")
internal class AdminTechBlogController(
    private val techBlogService: AdminTechBlogService
) {
    @PostMapping
    fun create(
        @RequestBody @Valid command: AdminCreateTechBlogCommand
    ): AdminApiResponse<AdminTechBlogData> {
        val response = techBlogService.create(command)

        return AdminApiResponse.of(response)
    }

    @PostMapping("/init")
    fun init(
        @RequestBody @Valid command: AdminInitTechBlogCommand
    ): AdminApiResponse<AdminInitTechBlogResult> {
        val response = techBlogService.initTechBlog(command)
        return AdminApiResponse.of(response)
    }

    @PatchMapping("/{techBlogId}")
    fun update(
        @PathVariable techBlogId: Long,
        @RequestBody @Valid command: AdminUpdateTechBlogCommand
    ): AdminApiResponse<AdminTechBlogData> {
        val response = techBlogService.update(techBlogId, command)
        return AdminApiResponse.of(response)
    }
}

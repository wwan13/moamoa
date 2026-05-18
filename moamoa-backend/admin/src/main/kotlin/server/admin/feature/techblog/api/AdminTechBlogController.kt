package server.admin.feature.techblog.api

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.*
import server.admin.feature.techblog.application.*
import server.admin.global.security.AdminPassport
import server.admin.global.security.RequestAdminPassport
import server.admin.global.security.ensureAdmin
import server.admin.global.web.AdminApiResponse

@RestController
@RequestMapping("/api/admin/tech-blog")
internal class AdminTechBlogController(
    private val techBlogService: AdminTechBlogService,
    private val techBlogCollectFacade: TechBlogCollectFacade,
) {
    @GetMapping
    fun findAll(
        @RequestAdminPassport passport: AdminPassport,
    ): AdminApiResponse<List<AdminTechBlogData>> {
        passport.ensureAdmin()
        val response = techBlogService.findAll()
        return AdminApiResponse.of(response)
    }

    @PostMapping
    fun create(
        @RequestBody @Valid command: AdminCreateTechBlogCommand
    ): AdminApiResponse<AdminTechBlogData> {
        val response = techBlogService.create(command)

        return AdminApiResponse.of(response)
    }

    @PostMapping("/collect-posts")
    fun collectPosts(
        @RequestBody @Valid command: AdminCollectPostsCommand
    ): AdminApiResponse<AdminCollectPostsResult> {
        val response = techBlogCollectFacade.collectPosts(command)
        return AdminApiResponse.of(response)
    }

    @DeleteMapping("/{techBlogId}/posts")
    fun deletePosts(
        @PathVariable techBlogId: Long,
    ): AdminApiResponse<AdminDeleteTechBlogPostsResult> {
        val response = techBlogService.deletePosts(techBlogId)
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

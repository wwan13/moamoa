package server.admin.feature.post.api

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import server.admin.feature.post.command.application.AdminPostService

@RestController
@RequestMapping("/api/admin/post")
internal class AdminPostController(
    private val postService: AdminPostService
) {
}

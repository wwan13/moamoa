package server.admin.presentation

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import server.admin.application.AdminInitPostsCommand
import server.admin.application.AdminInitPostsResult
import server.admin.application.AdminPostService

@RestController
@RequestMapping("/api/admin/post")
class AdminPostController(
    private val postService: AdminPostService
) {

    @PostMapping("init")
    suspend fun initPosts(
        @RequestBody command: AdminInitPostsCommand
    ): ResponseEntity<AdminInitPostsResult?> {
        val response = postService.initPosts(command)
        return ResponseEntity.ok(response)
    }
}
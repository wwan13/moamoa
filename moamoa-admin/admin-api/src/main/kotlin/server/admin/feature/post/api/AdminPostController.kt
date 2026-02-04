package server.admin.feature.post.api

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/post")
internal class AdminPostController(
    private val postService: server.admin.feature.post.application.AdminPostService
) {

    @PostMapping("init")
    suspend fun initPosts(
        @RequestBody @Valid command: server.admin.feature.post.application.AdminInitPostsCommand
    ): ResponseEntity<server.admin.feature.post.application.AdminInitPostsResult> {
        val response = postService.initPosts(command)
        return ResponseEntity.ok(response)
    }
}

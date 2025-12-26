package server.presentation

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import server.application.PostData
import server.application.PostService

@RestController
@RequestMapping("/api/post")
class PostController(
    private val postService: PostService
) {

    @GetMapping("/{postId}")
    suspend fun findById(
        @PathVariable postId: Long
    ): ResponseEntity<PostData> {
        val response = postService.findById(postId)

        return ResponseEntity.ok(response)
    }
}

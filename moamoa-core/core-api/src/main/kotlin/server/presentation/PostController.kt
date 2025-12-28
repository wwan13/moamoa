package server.presentation

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import server.application.IncreaseViewCountResult
import server.application.PostData
import server.application.PostList
import server.application.PostQueryConditions
import server.application.PostService

@RestController
@RequestMapping("/api/post")
class PostController(
    private val postService: PostService
) {

    @PostMapping("/{postId}/view")
    suspend fun increaseViewCount(
        @PathVariable postId: Long
    ): ResponseEntity<IncreaseViewCountResult?> {
        val response = postService.increaseViewCount(postId)

        return ResponseEntity.ok(response)
    }

    @GetMapping
    suspend fun findByConditions(
        postQueryConditions: PostQueryConditions
    ): ResponseEntity<PostList> {
        val response = postService.findByConditions(postQueryConditions)

        return ResponseEntity.ok(response)
    }
}

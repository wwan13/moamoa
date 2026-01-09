package server.feature.post.api

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import server.feature.post.command.application.IncreaseViewCountResult
import server.feature.post.command.application.PostService
import server.feature.post.query.PostList
import server.feature.post.query.PostQueryConditions
import server.feature.post.query.PostQueryService
import server.security.Passport
import server.security.RequestPassport

@RestController
@RequestMapping("/api/post")
class PostController(
    private val postService: PostService,
    private val postQueryService: PostQueryService,
) {

    @PostMapping("/{postId}/view")
    suspend fun increaseViewCount(
        @PathVariable postId: Long
    ): ResponseEntity<IncreaseViewCountResult> {
        val response = postService.increaseViewCount(postId)

        return ResponseEntity.ok(response)
    }

    @GetMapping
    suspend fun findByConditions(
        postQueryConditions: PostQueryConditions,
        @RequestPassport passport: Passport?
    ): ResponseEntity<PostList> {
        val response = postQueryService.findByConditions(postQueryConditions, passport)

        return ResponseEntity.ok(response)
    }
}

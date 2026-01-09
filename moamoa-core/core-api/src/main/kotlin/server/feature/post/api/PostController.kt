package server.feature.post.api

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import server.feature.post.command.application.IncreaseViewCountResult
import server.feature.post.command.application.PostService
import server.feature.post.query.BookmarkPostQueryService
import server.feature.post.query.PostList
import server.feature.post.query.PostQueryConditions
import server.feature.post.query.PostQueryService
import server.feature.post.query.SubscribingPostQueryService
import server.feature.post.query.TechBlogPostQueryConditions
import server.feature.post.query.TechBlogPostQueryService
import server.security.Passport
import server.security.RequestPassport

@RestController
@RequestMapping("/api/post")
class PostController(
    private val postService: PostService,
    private val postQueryService: PostQueryService,
    private val techBlogPostQueryService: TechBlogPostQueryService,
    private val subscribingPostQueryService: SubscribingPostQueryService,
    private val bookmarkPostQueryService: BookmarkPostQueryService,
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
        conditions: PostQueryConditions,
        @RequestPassport passport: Passport?
    ): ResponseEntity<PostList> {
        val response = postQueryService.findByConditions(conditions, passport)

        return ResponseEntity.ok(response)
    }

    @GetMapping("/tech-blog")
    suspend fun findAllByTechBlogConditions(
        conditions: TechBlogPostQueryConditions,
        @RequestPassport passport: Passport?
    ): ResponseEntity<PostList> {
        val response = techBlogPostQueryService.findAllByConditions(conditions, passport)

        return ResponseEntity.ok(response)
    }

    @GetMapping("/subscription")
    suspend fun findAllBySubscribingConditions(
        conditions: PostQueryConditions,
        @RequestPassport passport: Passport
    ): ResponseEntity<PostList> {
        val response = subscribingPostQueryService.findAllByConditions(conditions, passport)

        return ResponseEntity.ok(response)
    }

    @GetMapping("/bookmark")
    suspend fun findAllByBookmarkConditions(
        conditions: PostQueryConditions,
        @RequestPassport passport: Passport
    ): ResponseEntity<PostList> {
        val response = bookmarkPostQueryService.findAllByConditions(conditions, passport)

        return ResponseEntity.ok(response)
    }
}

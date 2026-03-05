package server.core.feature.post.api

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import server.core.feature.post.application.IncreaseViewCountResult
import server.core.feature.post.application.PostService
import server.core.feature.post.query.BookmarkedPostQueryService
import server.core.feature.post.query.PostList
import server.core.feature.post.query.PostQueryConditions
import server.core.feature.post.query.PostQueryService
import server.core.feature.post.query.SubscribedPostQueryService
import server.core.feature.post.query.TechBlogPostQueryConditions
import server.core.feature.post.query.TechBlogPostQueryService
import server.core.global.security.Passport
import server.core.global.security.RequestPassport

@RestController
@RequestMapping("/api/post")
class PostController(
    private val postService: PostService,
    private val postQueryService: PostQueryService,
    private val techBlogPostQueryService: TechBlogPostQueryService,
    private val subscribedPostQueryService: SubscribedPostQueryService,
    private val bookmarkedPostQueryService: BookmarkedPostQueryService,
) {

    @PostMapping("/{postId}/view")
    fun increaseViewCount(
        @PathVariable postId: Long
    ): ResponseEntity<IncreaseViewCountResult> {
        val response = postService.increaseViewCount(postId)

        return ResponseEntity.ok(response)
    }

    @GetMapping
    fun findByConditions(
        conditions: PostQueryConditions,
        @RequestPassport passport: Passport?
    ): ResponseEntity<PostList> {
        val response = postQueryService.findByConditions(conditions, passport)

        return ResponseEntity.ok(response)
    }

    @GetMapping("/tech-blog")
    fun findAllByTechBlogConditions(
        conditions: TechBlogPostQueryConditions,
        @RequestPassport passport: Passport?
    ): ResponseEntity<PostList> {
        val response = techBlogPostQueryService.findAllByConditions(conditions, passport)

        return ResponseEntity.ok(response)
    }

    @GetMapping("/subscribed")
    fun findAllBySubscribedConditions(
        conditions: PostQueryConditions,
        @RequestPassport passport: Passport
    ): ResponseEntity<PostList> {
        val response = subscribedPostQueryService.findAllByConditions(conditions, passport)

        return ResponseEntity.ok(response)
    }

    @GetMapping("/bookmarked")
    fun findAllByBookmarkedConditions(
        conditions: PostQueryConditions,
        @RequestPassport passport: Passport
    ): ResponseEntity<PostList> {
        val response = bookmarkedPostQueryService.findAllByConditions(conditions, passport)

        return ResponseEntity.ok(response)
    }
}

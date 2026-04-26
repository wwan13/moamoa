package server.core.feature.post.api

import org.springframework.web.bind.annotation.*
import server.core.feature.post.application.PostData
import server.core.feature.post.application.PostService
import server.core.feature.post.query.*
import server.core.global.security.Passport
import server.core.global.security.RequestPassport
import server.core.global.web.ApiResponse

@RestController
@RequestMapping("/api/post")
class PostController(
    private val postService: PostService,
    private val postQueryService: PostQueryService,
    private val techBlogPostQueryService: TechBlogPostQueryService,
    private val subscribedPostQueryService: SubscribedPostQueryService,
    private val bookmarkedPostQueryService: BookmarkedPostQueryService,
) {

    @PostMapping("/{postId}")
    fun findById(
        @PathVariable postId: Long
    ): ApiResponse<PostData> {
        val response = postService.findById(postId)

        return ApiResponse.of(response)
    }

    @GetMapping
    fun findByConditions(
        conditions: PostQueryConditions,
        @RequestPassport passport: Passport?
    ): ApiResponse<PostList> {
        val response = postQueryService.findByConditions(conditions, passport)

        return ApiResponse.of(response)
    }

    @GetMapping("/tech-blog")
    fun findAllByTechBlogConditions(
        conditions: TechBlogPostQueryConditions,
        @RequestPassport passport: Passport?
    ): ApiResponse<PostList> {
        val response = techBlogPostQueryService.findAllByConditions(conditions, passport)

        return ApiResponse.of(response)
    }

    @GetMapping("/subscribed")
    fun findAllBySubscribedConditions(
        conditions: PostQueryConditions,
        @RequestPassport passport: Passport
    ): ApiResponse<PostList> {
        val response = subscribedPostQueryService.findAllByConditions(conditions, passport)

        return ApiResponse.of(response)
    }

    @GetMapping("/bookmarked")
    fun findAllByBookmarkedConditions(
        conditions: PostQueryConditions,
        @RequestPassport passport: Passport
    ): ApiResponse<PostList> {
        val response = bookmarkedPostQueryService.findAllByConditions(conditions, passport)

        return ApiResponse.of(response)
    }
}

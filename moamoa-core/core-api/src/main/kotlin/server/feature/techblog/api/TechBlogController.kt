package server.feature.techblog.api

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import server.feature.techblog.command.application.TechBlogData
import server.feature.techblog.command.application.TechBlogService
import server.feature.techblog.query.SubscribedTechBlogQueryService
import server.feature.techblog.query.TechBlogList
import server.feature.techblog.query.TechBlogQueryConditions
import server.feature.techblog.query.TechBlogQueryService
import server.feature.techblog.query.TechBlogSummary
import server.security.Passport
import server.security.RequestPassport

@RestController
@RequestMapping("/api/tech-blog")
class TechBlogController(
    private val techBlogService: TechBlogService,
    private val techBlogQueryService: TechBlogQueryService,
    private val subscribedTechBlogQueryService: SubscribedTechBlogQueryService
) {

    @GetMapping("/{techBlogId}")
    suspend fun findById(
        @PathVariable techBlogId: Long,
        @RequestPassport passport: Passport?,
    ): ResponseEntity<TechBlogSummary> {
        val response = techBlogQueryService.findById(passport, techBlogId)
        return ResponseEntity.ok(response)
    }

    @GetMapping
    suspend fun findAll(
        conditions: TechBlogQueryConditions,
        @RequestPassport passport: Passport?,
    ): ResponseEntity<TechBlogList> {
        val response = techBlogQueryService.findAll(passport, conditions)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/subscription")
    suspend fun findSubscribingTechBlogs(
        @RequestPassport passport: Passport
    ): ResponseEntity<TechBlogList> {
        val response = subscribedTechBlogQueryService.findSubscribingTechBlogs(passport)
        return ResponseEntity.ok(response)
    }
}
package server.core.feature.techblog.api

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import server.core.feature.techblog.application.TechBlogService
import server.core.feature.techblog.query.SubscribedTechBlogQueryService
import server.core.feature.techblog.query.TechBlogList
import server.core.feature.techblog.query.TechBlogQueryConditions
import server.core.feature.techblog.query.TechBlogQueryService
import server.core.feature.techblog.query.TechBlogSummary
import server.core.global.security.Passport
import server.core.global.security.RequestPassport

@RestController
@RequestMapping("/api/tech-blog")
class TechBlogController(
    private val techBlogService: TechBlogService,
    private val techBlogQueryService: TechBlogQueryService,
    private val subscribedTechBlogQueryService: SubscribedTechBlogQueryService
) {

    @GetMapping("/{techBlogId}")
    fun findById(
        @PathVariable techBlogId: Long,
        @RequestPassport passport: Passport?,
    ): ResponseEntity<TechBlogSummary> {
        val response = techBlogQueryService.findById(passport, techBlogId)
        return ResponseEntity.ok(response)
    }

    @GetMapping
    fun findAll(
        conditions: TechBlogQueryConditions,
        @RequestPassport passport: Passport?,
    ): ResponseEntity<TechBlogList> {
        val response = techBlogQueryService.findAll(passport, conditions)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/subscription")
    fun findSubscribingTechBlogs(
        @RequestPassport passport: Passport
    ): ResponseEntity<TechBlogList> {
        val response = subscribedTechBlogQueryService.findSubscribingTechBlogs(passport)
        return ResponseEntity.ok(response)
    }
}

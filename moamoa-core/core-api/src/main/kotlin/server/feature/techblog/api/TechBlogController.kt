package server.feature.techblog.api

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import server.feature.techblog.command.application.TechBlogData
import server.feature.techblog.command.application.TechBlogService
import server.feature.techblog.query.SubscribedTechBlogQueryService
import server.feature.techblog.query.TechBlogList
import server.feature.techblog.query.TechBlogQueryService
import server.security.Passport
import server.security.RequestPassport

@RestController
@RequestMapping("/api/tech-blog")
class TechBlogController(
    private val techBlogService: TechBlogService,
    private val techBlogQueryService: TechBlogQueryService,
    private val subscribedTechBlogQueryService: SubscribedTechBlogQueryService
) {

    @GetMapping("/{techBlogKey}")
    suspend fun findById(
        @PathVariable techBlogKey: String
    ): ResponseEntity<TechBlogData> {
        val response = techBlogService.findByKey(techBlogKey)
        return ResponseEntity.ok(response)
    }

    @GetMapping
    suspend fun findAll(
        @RequestPassport passport: Passport?
    ): ResponseEntity<TechBlogList> {
        val response = techBlogQueryService.findAll(passport)
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
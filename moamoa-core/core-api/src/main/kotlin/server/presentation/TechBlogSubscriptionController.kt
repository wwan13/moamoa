package server.presentation

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import server.application.TechBlogSubscriptionService
import server.application.TechBlogSubscriptionToggleCommand
import server.application.TechBlogSubscriptionToggleResult
import server.security.Passport
import server.security.RequestPassport

@RestController
@RequestMapping("/api/tech-blog-subscription")
class TechBlogSubscriptionController(
    private val techBlogSubscriptionService: TechBlogSubscriptionService
) {

    @PostMapping
    suspend fun toggle(
        @RequestBody command: TechBlogSubscriptionToggleCommand,
        @RequestPassport passport: Passport
    ): ResponseEntity<TechBlogSubscriptionToggleResult> {
        val response = techBlogSubscriptionService.toggle(command, passport.memberId)

        return ResponseEntity.ok(response)
    }
}
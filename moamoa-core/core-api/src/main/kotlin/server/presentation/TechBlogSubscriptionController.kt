package server.presentation

import jakarta.validation.Valid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import server.application.NotificationEnabledToggleCommand
import server.application.NotificationEnabledToggleResult
import server.application.TechBlogData
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
        @RequestBody @Valid command: TechBlogSubscriptionToggleCommand,
        @RequestPassport passport: Passport
    ): ResponseEntity<TechBlogSubscriptionToggleResult> {
        val response = techBlogSubscriptionService.toggle(command, passport.memberId)

        return ResponseEntity.ok(response)
    }

    @PatchMapping("/notification-enabled")
    suspend fun notificationEnabledToggle(
        @RequestBody @Valid command: NotificationEnabledToggleCommand,
        @RequestPassport passport: Passport
    ): ResponseEntity<NotificationEnabledToggleResult> {
        val response = techBlogSubscriptionService.notificationEnabledToggle(command, passport.memberId)

        return ResponseEntity.ok(response)
    }

    @GetMapping
    suspend fun subscribingTechBlogs(
        @RequestPassport passport: Passport
    ): ResponseEntity<List<TechBlogData>> {
        val response = techBlogSubscriptionService.subscribingTechBlogs(passport.memberId).toList()

        return ResponseEntity.ok(response)
    }
}
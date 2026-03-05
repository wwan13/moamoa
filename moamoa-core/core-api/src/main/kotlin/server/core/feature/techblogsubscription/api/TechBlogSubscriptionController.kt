package server.core.feature.techblogsubscription.api

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import server.core.feature.techblog.application.TechBlogData
import server.core.feature.techblogsubscription.application.NotificationEnabledToggleCommand
import server.core.feature.techblogsubscription.application.NotificationEnabledToggleResult
import server.core.feature.techblogsubscription.application.TechBlogSubscriptionService
import server.core.feature.techblogsubscription.application.TechBlogSubscriptionToggleCommand
import server.core.feature.techblogsubscription.application.TechBlogSubscriptionToggleResult
import server.core.global.security.Passport
import server.core.global.security.RequestPassport

@RestController
@RequestMapping("/api/tech-blog-subscription")
class TechBlogSubscriptionController(
    private val techBlogSubscriptionService: TechBlogSubscriptionService
) {

    @PostMapping
    fun toggle(
        @RequestBody @Valid command: TechBlogSubscriptionToggleCommand,
        @RequestPassport passport: Passport
    ): ResponseEntity<TechBlogSubscriptionToggleResult> {
        val response = techBlogSubscriptionService.toggle(command, passport.memberId)

        return ResponseEntity.ok(response)
    }

    @PatchMapping("/notification-enabled")
    fun notificationEnabledToggle(
        @RequestBody @Valid command: NotificationEnabledToggleCommand,
        @RequestPassport passport: Passport
    ): ResponseEntity<NotificationEnabledToggleResult> {
        val response = techBlogSubscriptionService.notificationEnabledToggle(command, passport.memberId)

        return ResponseEntity.ok(response)
    }

    @GetMapping
    fun subscribingTechBlogs(
        @RequestPassport passport: Passport
    ): ResponseEntity<List<TechBlogData>> {
        val response = techBlogSubscriptionService.subscribingTechBlogs(passport.memberId)

        return ResponseEntity.ok(response)
    }
}

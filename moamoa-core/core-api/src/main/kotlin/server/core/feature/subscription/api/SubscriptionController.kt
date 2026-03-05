package server.core.feature.subscription.api

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import server.core.feature.techblog.application.TechBlogData
import server.core.feature.subscription.application.NotificationEnabledToggleCommand
import server.core.feature.subscription.application.NotificationEnabledToggleResult
import server.core.feature.subscription.application.SubscriptionService
import server.core.feature.subscription.application.SubscriptionToggleCommand
import server.core.feature.subscription.application.SubscriptionToggleResult
import server.core.global.security.Passport
import server.core.global.security.RequestPassport

@RestController
@RequestMapping("/api/subscription")
class SubscriptionController(
    private val subscriptionService: SubscriptionService
) {

    @PostMapping
    fun toggle(
        @RequestBody @Valid command: SubscriptionToggleCommand,
        @RequestPassport passport: Passport
    ): ResponseEntity<SubscriptionToggleResult> {
        val response = subscriptionService.toggle(command, passport.memberId)

        return ResponseEntity.ok(response)
    }

    @PatchMapping("/notification-enabled")
    fun notificationEnabledToggle(
        @RequestBody @Valid command: NotificationEnabledToggleCommand,
        @RequestPassport passport: Passport
    ): ResponseEntity<NotificationEnabledToggleResult> {
        val response = subscriptionService.notificationEnabledToggle(command, passport.memberId)

        return ResponseEntity.ok(response)
    }
}

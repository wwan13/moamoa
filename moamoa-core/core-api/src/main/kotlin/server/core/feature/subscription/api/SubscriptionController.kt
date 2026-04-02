package server.core.feature.subscription.api

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import server.core.feature.subscription.application.NotificationEnabledResult
import server.core.feature.subscription.application.SubscriptionCommand
import server.core.feature.subscription.application.SubscriptionResult
import server.core.feature.subscription.application.SubscriptionService
import server.core.global.security.Passport
import server.core.global.security.RequestPassport

@RestController
@RequestMapping("/api/subscription")
class SubscriptionController(
    private val subscriptionService: SubscriptionService
) {

    @PostMapping
    fun subscribe(
        @RequestBody @Valid command: SubscriptionCommand,
        @RequestPassport passport: Passport
    ): ResponseEntity<SubscriptionResult> {
        val response = subscriptionService.subscribe(command, passport.memberId)

        return ResponseEntity.ok(response)
    }

    @DeleteMapping
    fun unsubscribe(
        @RequestBody @Valid command: SubscriptionCommand,
        @RequestPassport passport: Passport
    ): ResponseEntity<SubscriptionResult> {
        val response = subscriptionService.unsubscribe(command, passport.memberId)

        return ResponseEntity.ok(response)
    }

    @PostMapping("/notification-enabled")
    fun enableNotification(
        @RequestBody @Valid command: SubscriptionCommand,
        @RequestPassport passport: Passport
    ): ResponseEntity<NotificationEnabledResult> {
        val response = subscriptionService.enableNotification(command, passport.memberId)

        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/notification-enabled")
    fun disableNotification(
        @RequestBody @Valid command: SubscriptionCommand,
        @RequestPassport passport: Passport
    ): ResponseEntity<NotificationEnabledResult> {
        val response = subscriptionService.disableNotification(command, passport.memberId)

        return ResponseEntity.ok(response)
    }
}

package server.core.feature.subscription.api

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import server.core.feature.subscription.application.SubscriptionCommand
import server.core.feature.subscription.application.SubscriptionService
import server.core.global.security.Passport
import server.core.global.security.RequestPassport
import server.core.global.web.ApiResponse

@RestController
@RequestMapping("/api/subscription")
class SubscriptionController(
    private val subscriptionService: SubscriptionService
) {

    @PostMapping
    fun subscribe(
        @RequestBody @Valid command: SubscriptionCommand,
        @RequestPassport passport: Passport
    ): ApiResponse<Unit> {
        subscriptionService.subscribe(command, passport.memberId)

        return ApiResponse.of()
    }

    @DeleteMapping
    fun unsubscribe(
        @RequestBody @Valid command: SubscriptionCommand,
        @RequestPassport passport: Passport
    ): ApiResponse<Unit> {
        subscriptionService.unsubscribe(command, passport.memberId)

        return ApiResponse.of()
    }

    @PostMapping("/notification-enabled")
    fun enableNotification(
        @RequestBody @Valid command: SubscriptionCommand,
        @RequestPassport passport: Passport
    ): ApiResponse<Unit> {
        subscriptionService.enableNotification(command, passport.memberId)

        return ApiResponse.of()
    }

    @DeleteMapping("/notification-enabled")
    fun disableNotification(
        @RequestBody @Valid command: SubscriptionCommand,
        @RequestPassport passport: Passport
    ): ApiResponse<Unit> {
        subscriptionService.disableNotification(command, passport.memberId)

        return ApiResponse.of()
    }
}

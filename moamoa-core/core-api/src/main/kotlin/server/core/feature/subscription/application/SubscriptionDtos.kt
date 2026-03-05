package server.core.feature.subscription.application

data class SubscriptionToggleCommand(
    val techBlogId: Long
)

data class SubscriptionToggleResult(
    val subscribing: Boolean
)

data class NotificationEnabledToggleCommand(
    val techBlogId: Long
)

data class NotificationEnabledToggleResult(
    val notificationEnabled: Boolean
)
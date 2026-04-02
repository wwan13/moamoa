package server.core.feature.subscription.application

data class SubscriptionCommand(
    val techBlogId: Long
)

data class SubscriptionResult(
    val subscribing: Boolean
)

data class NotificationEnabledResult(
    val notificationEnabled: Boolean
)

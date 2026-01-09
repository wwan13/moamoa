package server.feature.techblogsubscription.application

data class TechBlogSubscriptionToggleCommand(
    val techBlogId: Long
)

data class TechBlogSubscriptionToggleResult(
    val subscribing: Boolean
)

data class NotificationEnabledToggleCommand(
    val techBlogId: Long
)

data class NotificationEnabledToggleResult(
    val notificationEnabled: Boolean
)
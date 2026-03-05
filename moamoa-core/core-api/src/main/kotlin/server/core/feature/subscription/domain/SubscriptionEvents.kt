package server.core.feature.subscription.domain

data class TechBlogSubscribeUpdatedEvent(
    val memberId: Long,
    val techBlogId: Long,
    val subscribed: Boolean
)

data class NotificationUpdatedEvent(
    val memberId: Long,
    val techBlogId: Long,
    val enabled: Boolean
)

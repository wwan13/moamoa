package server.feature.techblogsubscription.domain

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

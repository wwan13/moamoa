package server.core.feature.subscription.domain

import server.messaging.Event

data class TechBlogSubscribeUpdatedEvent(
    val memberId: Long,
    val techBlogId: Long,
    val subscribed: Boolean
) : Event

data class NotificationUpdatedEvent(
    val memberId: Long,
    val techBlogId: Long,
    val enabled: Boolean
) : Event

package server.core.feature.subscription.domain

import server.core.support.domain.DomainEvent

data class TechBlogSubscribeUpdatedEvent(
    val memberId: Long,
    val techBlogId: Long,
    val subscribed: Boolean
) : DomainEvent

data class NotificationUpdatedEvent(
    val memberId: Long,
    val techBlogId: Long,
    val enabled: Boolean
) : DomainEvent

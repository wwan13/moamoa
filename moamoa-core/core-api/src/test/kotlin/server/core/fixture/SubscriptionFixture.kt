package server.core.fixture

import server.core.feature.subscription.domain.Subscription

fun createSubscription(
    id: Long = 0L,
    notificationEnabled: Boolean = true,
    memberId: Long = 1L,
    techBlogId: Long = 1L
): Subscription = Subscription(
    id = id,
    notificationEnabled = notificationEnabled,
    memberId = memberId,
    techBlogId = techBlogId
)

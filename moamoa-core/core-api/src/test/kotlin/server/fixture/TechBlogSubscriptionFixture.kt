package server.fixture

import server.feature.techblogsubscription.domain.TechBlogSubscription

fun createTechBlogSubscription(
    id: Long = 0L,
    notificationEnabled: Boolean = true,
    memberId: Long = 1L,
    techBlogId: Long = 1L
): TechBlogSubscription = TechBlogSubscription(
    id = id,
    notificationEnabled = notificationEnabled,
    memberId = memberId,
    techBlogId = techBlogId
)

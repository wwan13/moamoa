package server.feature.techblogsubscription.domain

data class TechBlogSubscribeCreatedEvent(
    val memberId: Long,
    val techBlogId: Long
)

data class TechBlogSubscribeRemovedEvent(
    val memberId: Long,
    val techBlogId: Long
)
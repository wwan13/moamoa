package server.feature.techblog.command.domain

data class TechBlogSubscribeCreatedEvent(
    val memberId: Long,
    val techBlogId: Long
)

data class TechBlogSubscribeRemovedEvent(
    val memberId: Long,
    val techBlogId: Long
)
package server.feature.techblog.command.domain

data class TechBlogSubscribeCreatedEvent(
    val techBlogId: Long
)

data class TechBlogSubscribeRemovedEvent(
    val techBlogId: Long
)
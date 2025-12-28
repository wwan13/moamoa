package server.domain.techblog

data class TechBlogSubscribeCreatedEvent(
    val techBlogId: Long
)

data class TechBlogSubscribeRemovedEvent(
    val techBlogId: Long
)
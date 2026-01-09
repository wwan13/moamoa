package server.feature.techblog.domain

data class TechBlogSubscribeCreatedEvent(
    val techBlogId: Long
)

data class TechBlogSubscribeRemovedEvent(
    val techBlogId: Long
)
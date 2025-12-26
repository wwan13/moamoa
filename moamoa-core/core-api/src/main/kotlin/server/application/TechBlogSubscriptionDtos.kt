package server.application

data class TechBlogSubscriptionToggleCommand(
    val techBlogId: Long
)

data class TechBlogSubscriptionToggleResult(
    val subscribing: Boolean
)
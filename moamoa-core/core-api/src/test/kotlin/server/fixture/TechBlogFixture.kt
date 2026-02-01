package server.fixture

import server.feature.techblog.command.domain.TechBlog

fun createTechBlog(
    id: Long = 0L,
    title: String = "Blog",
    key: String = "key",
    icon: String = "icon",
    blogUrl: String = "https://example.com",
    subscriptionCount: Long = 0L
): TechBlog = TechBlog(
    id = id,
    title = title,
    key = key,
    icon = icon,
    blogUrl = blogUrl,
    subscriptionCount = subscriptionCount
)

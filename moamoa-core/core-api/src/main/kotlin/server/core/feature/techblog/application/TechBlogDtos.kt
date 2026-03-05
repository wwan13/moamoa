package server.core.feature.techblog.application

import server.core.feature.techblog.domain.TechBlog

data class TechBlogData(
    val id: Long,
    val title: String,
    val icon: String,
    val blogUrl: String,
    val key: String,
    val subscriptionCount: Long
) {
    constructor(
        techBlog: TechBlog
    ) : this(
        id = techBlog.id,
        title = techBlog.title,
        key = techBlog.key,
        icon = techBlog.icon,
        blogUrl = techBlog.blogUrl,
        subscriptionCount = techBlog.subscriptionCount
    )
}
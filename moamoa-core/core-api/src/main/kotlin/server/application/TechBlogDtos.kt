package server.application

import server.domain.techblog.TechBlog

data class TechBlogData(
    val id: Long,
    val title: String,
    val icon: String,
    val blogUrl: String,
    val key: String
) {
    constructor(
        techBlog: TechBlog
    ) : this(
        id = techBlog.id,
        title = techBlog.title,
        key = techBlog.key,
        icon = techBlog.icon,
        blogUrl = techBlog.blogUrl
    )
}
package server.admin.application

import server.admin.domain.techblog.AdminTechBlog

data class AdminCreateTechBlogCommand(
    val title: String,
    val key: String,
    val icon: String,
    val blogUrl: String
)

data class AdminUpdateTechBlogCommand(
    val title: String,
    val icon: String,
    val blogUrl: String
)

data class AdminTechBlogData(
    val id: Long,
    val title: String,
    val icon: String,
    val blogUrl: String,
    val key: String
) {
    constructor(
        techBlog: AdminTechBlog
    ) : this(
        id = techBlog.id,
        title = techBlog.title,
        key = techBlog.key,
        icon = techBlog.icon,
        blogUrl = techBlog.blogUrl
    )
}
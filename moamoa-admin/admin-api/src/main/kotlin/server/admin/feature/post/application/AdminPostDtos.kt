package server.admin.feature.post.application

internal data class AdminInitPostsCommand(
    val techBlogId: Long
)

internal data class AdminInitPostsResult(
    val techBlog: server.admin.feature.techblog.application.AdminTechBlogData,
    val newPostCount: Int
)

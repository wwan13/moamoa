package server.admin.application

internal data class AdminInitPostsCommand(
    val techBlogId: Long
)

internal data class AdminInitPostsResult(
    val techBlog: AdminTechBlogData,
    val newPostCount: Int
)

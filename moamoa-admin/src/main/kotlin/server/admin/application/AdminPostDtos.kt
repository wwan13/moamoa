package server.admin.application

data class AdminInitPostsCommand(
    val techBlogId: Long
)

data class AdminInitPostsResult(
    val techBlog: AdminTechBlogData,
    val newPostCount: Int
)

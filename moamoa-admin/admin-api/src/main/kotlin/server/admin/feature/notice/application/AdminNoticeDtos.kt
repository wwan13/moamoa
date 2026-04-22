package server.admin.feature.notice.application

data class AdminNoticeCreateCommand(
    val title: String,
    val chip: String,
    val content: String,
    val isNewBlog: Boolean,
)

data class AdminUpdateNoticePublishedCommand(
    val published: Boolean,
)

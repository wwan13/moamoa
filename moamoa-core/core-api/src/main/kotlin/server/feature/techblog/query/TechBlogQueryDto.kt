package server.feature.techblog.query

data class TechBlogList(
    val meta: TechBlogListMeta,
    val techBlogs: List<TechBlogSummary>
)

data class TechBlogListMeta(
    val totalCount: Long,
)

data class TechBlogSummary(
    val id: Long,
    val title: String,
    val icon: String,
    val blogUrl: String,
    val key: String,
    val subscriptionCount: Long,
    val postCount: Long,
    val subscribed: Boolean,
    val notificationEnabled: Boolean,
)

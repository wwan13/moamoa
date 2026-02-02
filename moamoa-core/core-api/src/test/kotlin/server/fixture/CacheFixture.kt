package server.fixture

import server.feature.post.query.PostStats
import server.feature.post.query.PostSummary
import server.feature.techblog.command.application.TechBlogData
import server.feature.techblog.query.TechBlogSubscriptionInfo
import server.feature.techblog.query.TechBlogSummary
import java.time.LocalDateTime

fun createTechBlogData(
    id: Long = 1L,
    title: String = "Blog",
    icon: String = "icon",
    blogUrl: String = "https://example.com",
    key: String = "key",
    subscriptionCount: Long = 0L
): TechBlogData = TechBlogData(
    id = id,
    title = title,
    icon = icon,
    blogUrl = blogUrl,
    key = key,
    subscriptionCount = subscriptionCount
)

fun createPostSummary(
    id: Long = 1L,
    key: String = "post-key",
    title: String = "title",
    description: String = "description",
    thumbnail: String = "thumbnail",
    url: String = "https://example.com/post",
    publishedAt: LocalDateTime = LocalDateTime.of(2024, 1, 1, 0, 0),
    isBookmarked: Boolean = false,
    viewCount: Long = 0L,
    bookmarkCount: Long = 0L,
    techBlog: TechBlogData = createTechBlogData()
): PostSummary = PostSummary(
    id = id,
    key = key,
    title = title,
    description = description,
    thumbnail = thumbnail,
    url = url,
    publishedAt = publishedAt,
    isBookmarked = isBookmarked,
    viewCount = viewCount,
    bookmarkCount = bookmarkCount,
    techBlog = techBlog
)

fun createPostStats(
    postId: Long = 1L,
    viewCount: Long = 0L,
    bookmarkCount: Long = 0L
): PostStats = PostStats(
    postId = postId,
    viewCount = viewCount,
    bookmarkCount = bookmarkCount
)

fun createTechBlogSummary(
    id: Long = 1L,
    title: String = "Blog",
    icon: String = "icon",
    blogUrl: String = "https://example.com",
    key: String = "key",
    subscriptionCount: Long = 0L,
    postCount: Long = 0L,
    subscribed: Boolean = false,
    notificationEnabled: Boolean = false
): TechBlogSummary = TechBlogSummary(
    id = id,
    title = title,
    icon = icon,
    blogUrl = blogUrl,
    key = key,
    subscriptionCount = subscriptionCount,
    postCount = postCount,
    subscribed = subscribed,
    notificationEnabled = notificationEnabled
)

fun createTechBlogSubscriptionInfo(
    techBlogId: Long = 1L,
    subscribed: Boolean = true,
    notificationEnabled: Boolean = true
): TechBlogSubscriptionInfo = TechBlogSubscriptionInfo(
    techBlogId = techBlogId,
    subscribed = subscribed,
    notificationEnabled = notificationEnabled
)

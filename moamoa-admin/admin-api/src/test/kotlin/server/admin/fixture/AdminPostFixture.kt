package server.admin.fixture

import server.admin.feature.post.command.domain.AdminPost
import java.time.LocalDateTime

internal fun createAdminPost(
    id: Long = 0L,
    key: String = "key",
    title: String = "title",
    description: String = "description",
    thumbnail: String = "thumbnail",
    url: String = "https://example.com",
    publishedAt: LocalDateTime = LocalDateTime.now(),
    techBlogId: Long = 1L,
    categoryId: Long = 1L,
): AdminPost = AdminPost(
    id = id,
    key = key,
    title = title,
    description = description,
    thumbnail = thumbnail,
    url = url,
    publishedAt = publishedAt,
    techBlogId = techBlogId,
    categoryId = categoryId,
)

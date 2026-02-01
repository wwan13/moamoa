package server.fixture

import server.feature.post.command.domain.Post
import java.time.LocalDateTime

fun createPost(
    id: Long = 0L,
    key: String = "key",
    title: String = "title",
    description: String = "description",
    thumbnail: String = "thumbnail",
    url: String = "https://example.com",
    publishedAt: LocalDateTime = LocalDateTime.now(),
    viewCount: Long = 0,
    bookmarkCount: Long = 0,
    techBlogId: Long = 1,
    categoryId: Long = 1
): Post = Post(
    id = id,
    key = key,
    title = title,
    description = description,
    thumbnail = thumbnail,
    url = url,
    publishedAt = publishedAt,
    viewCount = viewCount,
    bookmarkCount = bookmarkCount,
    techBlogId = techBlogId,
    categoryId = categoryId
)

package server.client.techblogs

import java.time.LocalDateTime

data class TechBlogPost(
    val key: String,
    val title: String,
    val description: String,
    val categories: List<String>,
    val thumbnail: String,
    val publishedAt: LocalDateTime,
    val url: String
)
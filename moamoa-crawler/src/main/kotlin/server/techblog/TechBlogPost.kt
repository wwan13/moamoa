package server.techblog

import java.time.LocalDateTime

data class TechBlogPost(
    val key: String,
    val title: String,
    val description: String,
    val tags: List<String>,
    val thumbnail: String,
    val publishedAt: LocalDateTime,
    val url: String
)
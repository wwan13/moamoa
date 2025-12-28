package server.techblog

import java.time.LocalDateTime

data class TechBlogPost(
    val key: String,
    val title: String,
    val description: String,
    val categories: List<String>,
    val thumbnail: String? = null,
    val publishedAt: LocalDateTime,
    val url: String
)
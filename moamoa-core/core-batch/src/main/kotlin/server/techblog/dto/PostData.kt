package server.techblog.dto

import java.time.LocalDateTime

data class PostData(
    val key: String,
    val title: String,
    val description: String,
    val categories: List<String>,
    val thumbnail: String,
    val publishedAt: LocalDateTime,
    val url: String,
    val techBlogId: Long
)
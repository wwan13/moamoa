package server.batch.techblog.dto

import java.time.LocalDateTime

internal data class PostData(
    val key: String,
    val title: String,
    val description: String,
    val tags: List<String>,
    val thumbnail: String,
    val publishedAt: LocalDateTime,
    val url: String,
    val techBlogId: Long
)
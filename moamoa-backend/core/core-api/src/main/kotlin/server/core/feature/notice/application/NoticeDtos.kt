package server.core.feature.notice.application

import server.core.feature.notice.domain.Notice
import java.time.LocalDateTime

data class NoticeData(
    val id: Long,
    val title: String,
    val chip: String,
    val content: String,
    val publishedAt: LocalDateTime,
) {
    constructor(notice: Notice) : this(
        id = notice.id,
        title = notice.title,
        chip = notice.chip,
        content = notice.content,
        publishedAt = notice.publishedAt,
    )
}

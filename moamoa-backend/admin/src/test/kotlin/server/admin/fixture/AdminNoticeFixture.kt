package server.admin.fixture

import server.admin.feature.notice.domain.AdminNotice
import java.time.LocalDateTime

internal fun createAdminNotice(
    id: Long = 0L,
    title: String = "title",
    chip: String = "chip",
    content: String = "content",
    published: Boolean = false,
    publishedAt: LocalDateTime = LocalDateTime.now(),
): AdminNotice = AdminNotice(
    id = id,
    title = title,
    chip = chip,
    content = content,
    published = published,
    publishedAt = publishedAt,
)


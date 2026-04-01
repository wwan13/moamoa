package server.core.feature.notice.query

import java.time.LocalDateTime

data class NoticeQueryConditions(
    val page: Long?,
    val size: Long?,
    val query: String?,
)

data class NoticeList(
    val meta: NoticeListMeta,
    val notices: List<NoticeSummary>,
)

data class NoticeListMeta(
    val page: Long,
    val size: Long,
    val totalCount: Long,
    val totalPages: Long,
)

data class NoticeSummary(
    val id: Long,
    val title: String,
    val chip: String,
    val content: String,
    val publishedAt: LocalDateTime,
)

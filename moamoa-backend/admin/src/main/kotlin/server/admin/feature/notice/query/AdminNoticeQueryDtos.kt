package server.admin.feature.notice.query

import java.time.LocalDateTime

internal data class AdminNoticeQueryConditions(
    val page: Long?,
    val size: Long?,
    val query: String?,
    val published: Boolean?,
)

internal data class AdminNoticeList(
    val meta: AdminNoticeListMeta,
    val notices: List<AdminNoticeSummary>,
)

internal data class AdminNoticeListMeta(
    val page: Long,
    val size: Long,
    val totalCount: Long,
    val totalPages: Long,
)

internal data class AdminNoticeSummary(
    val id: Long,
    val title: String,
    val chip: String,
    val content: String,
    val published: Boolean,
    val publishedAt: LocalDateTime,
)

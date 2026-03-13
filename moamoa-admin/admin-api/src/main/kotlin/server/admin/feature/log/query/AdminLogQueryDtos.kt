package server.admin.feature.log.query

import java.time.LocalDateTime

internal data class AdminLogQueryConditions(
    val logLevel: String?,
    val type: String?,
    val traceId: String?,
    val fromTimestamp: LocalDateTime?,
    val toTimestamp: LocalDateTime?,
    val size: Long?,
    val cursorTimestamp: LocalDateTime?,
    val cursorId: Long?,
)

internal data class AdminLogPage(
    val items: List<AdminLogSummary>,
    val nextCursor: AdminLogCursor?,
    val size: Long,
    val hasNext: Boolean,
)

internal data class AdminLogSummary(
    val id: Long,
    val timestamp: LocalDateTime,
    val logLevel: String,
    val traceId: String,
    val loggerName: String,
    val message: String,
    val type: String,
    val data: String,
)

internal data class AdminLogCursor(
    val timestamp: LocalDateTime,
    val id: Long,
)

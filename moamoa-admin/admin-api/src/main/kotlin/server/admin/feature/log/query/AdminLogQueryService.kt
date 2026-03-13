package server.admin.feature.log.query

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.jdbc.core.DataClassRowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
internal class AdminLogQueryService(
    @param:Qualifier("adminLogNamedParameterJdbcTemplate")
    @param:Lazy
    private val jdbc: NamedParameterJdbcTemplate,
) {

    fun findByConditions(conditions: AdminLogQueryConditions): AdminLogPage {
        val size = conditions.size?.takeIf { it > 0 }?.coerceAtMost(MAX_SIZE) ?: DEFAULT_SIZE
        val timeRange = resolveTimeRange(conditions)
        val filter = buildFilter(conditions, timeRange)

        val sql = """
            SELECT
                l.id AS id,
                l.timestamp AS timestamp,
                l.level AS logLevel,
                l.trace_id AS traceId,
                l.logger_name AS loggerName,
                l.message AS message,
                l.type AS type,
                l.data AS data
            FROM log l
            ${filter.whereClause}
            ORDER BY l.timestamp DESC, l.id DESC
            LIMIT :limit
        """.trimIndent()

        val params = MapSqlParameterSource(filter.params)
            .addValue("limit", size + 1L)

        val rows = jdbc.query(sql, params, DataClassRowMapper.newInstance(AdminLogSummary::class.java))
        val hasNext = rows.size > size
        val pageRows = if (hasNext) rows.take(size.toInt()) else rows
        val items = pageRows
        val nextCursor = if (!hasNext) null else pageRows.lastOrNull()?.let {
            AdminLogCursor(
                timestamp = it.timestamp,
                id = it.id,
            )
        }

        return AdminLogPage(
            items = items,
            nextCursor = nextCursor,
            size = size,
            hasNext = hasNext,
        )
    }

    private fun buildFilter(conditions: AdminLogQueryConditions, timeRange: TimeRange): LogFilter {
        val whereClauses = mutableListOf<String>()
        val params = linkedMapOf<String, Any>()

        whereClauses += "l.timestamp >= :fromTimestamp"
        params["fromTimestamp"] = timeRange.fromTimestamp
        whereClauses += "l.timestamp <= :toTimestamp"
        params["toTimestamp"] = timeRange.toTimestamp

        conditions.logLevel?.let {
            whereClauses += "l.level = :logLevel"
            params["logLevel"] = it
        }
        conditions.type?.let {
            whereClauses += "l.type = :type"
            params["type"] = it
        }
        conditions.traceId?.takeIf { it.isNotBlank() }?.let {
            whereClauses += "l.trace_id LIKE :traceId"
            params["traceId"] = "%$it%"
        }

        if (conditions.cursorTimestamp != null && conditions.cursorId != null) {
            whereClauses += "(l.timestamp < :cursorTimestamp OR (l.timestamp = :cursorTimestamp AND l.id < :cursorId))"
            params["cursorTimestamp"] = conditions.cursorTimestamp
            params["cursorId"] = conditions.cursorId
        }

        val where = if (whereClauses.isEmpty()) "" else "WHERE ${whereClauses.joinToString(" AND ")}"
        return LogFilter(whereClause = where, params = params)
    }

    private fun resolveTimeRange(conditions: AdminLogQueryConditions): TimeRange {
        val now = LocalDateTime.now()
        val maxLookbackStart = now.toLocalDate().minusDays(MAX_LOOKBACK_DAYS).atStartOfDay()
        val toTimestamp = conditions.toTimestamp ?: now
        val fromTimestamp = conditions.fromTimestamp ?: toTimestamp.minusMinutes(DEFAULT_LOOKBACK_MINUTES)

        if (toTimestamp.isAfter(now)) {
            throw IllegalArgumentException("toTimestamp는 현재 시각 이후일 수 없습니다.")
        }
        if (fromTimestamp.isAfter(toTimestamp)) {
            throw IllegalArgumentException("fromTimestamp는 toTimestamp보다 이후일 수 없습니다.")
        }
        if (fromTimestamp.isBefore(maxLookbackStart)) {
            throw IllegalArgumentException("오늘 기준 7일보다 오래된 로그는 조회할 수 없습니다.")
        }

        return TimeRange(
            fromTimestamp = fromTimestamp,
            toTimestamp = toTimestamp,
        )
    }

    private data class LogFilter(
        val whereClause: String,
        val params: Map<String, Any>,
    )

    private data class TimeRange(
        val fromTimestamp: LocalDateTime,
        val toTimestamp: LocalDateTime,
    )

    private companion object {
        const val DEFAULT_SIZE = 100L
        const val MAX_SIZE = 100L
        const val DEFAULT_LOOKBACK_MINUTES = 10L
        const val MAX_LOOKBACK_DAYS = 7L
    }
}

package server.admin.feature.log.query

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.jdbc.core.DataClassRowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

@Service
internal class AdminLogQueryService(
    @param:Qualifier("adminLogNamedParameterJdbcTemplate")
    @param:Lazy
    private val jdbc: NamedParameterJdbcTemplate,
) {

    fun findByConditions(conditions: AdminLogQueryConditions): AdminLogPage {
        val size = conditions.size?.takeIf { it > 0 }?.coerceAtMost(MAX_SIZE) ?: DEFAULT_SIZE
        val filter = buildFilter(conditions)

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

    private fun buildFilter(conditions: AdminLogQueryConditions): LogFilter {
        val whereClauses = mutableListOf<String>()
        val params = linkedMapOf<String, Any>()

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

    private data class LogFilter(
        val whereClause: String,
        val params: Map<String, Any>,
    )

    private companion object {
        const val DEFAULT_SIZE = 100L
        const val MAX_SIZE = 100L
    }
}

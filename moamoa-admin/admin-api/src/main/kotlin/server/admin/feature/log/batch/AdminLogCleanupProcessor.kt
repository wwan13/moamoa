package server.admin.feature.log.batch

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Lazy
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
internal class AdminLogCleanupProcessor(
    @param:Qualifier("adminLogNamedParameterJdbcTemplate")
    @param:Lazy
    private val jdbc: NamedParameterJdbcTemplate,
) {

    fun deleteLogsOlderThan(retentionDays: Long = RETENTION_DAYS): Int {
        require(retentionDays > 0) { "retentionDays는 0보다 커야 합니다." }

        val cutoffTimestamp = LocalDateTime.now().minusDays(retentionDays)
        val sql = """
            DELETE FROM log
            WHERE timestamp < :cutoffTimestamp
        """.trimIndent()
        val params = MapSqlParameterSource("cutoffTimestamp", cutoffTimestamp)

        return jdbc.update(sql, params)
    }

    private companion object {
        const val RETENTION_DAYS = 7L
    }
}

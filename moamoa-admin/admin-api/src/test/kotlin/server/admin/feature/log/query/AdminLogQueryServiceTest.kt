package server.admin.feature.log.query

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.DataClassRowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import test.UnitTest
import java.time.LocalDateTime

class AdminLogQueryServiceTest : UnitTest() {

    @Test
    fun `필터가 없으면 기본 size 100과 limit 101로 조회한다`() {
        val jdbc = mockk<NamedParameterJdbcTemplate>()
        val service = AdminLogQueryService(jdbc)

        val sqlSlot = slot<String>()
        val paramsSlot = slot<MapSqlParameterSource>()

        every {
            jdbc.query(capture(sqlSlot), capture(paramsSlot), any<DataClassRowMapper<AdminLogSummary>>())
        } returns emptyList()

        val result = service.findByConditions(
            AdminLogQueryConditions(
                logLevel = null,
                type = null,
                traceId = null,
                size = null,
                cursorTimestamp = null,
                cursorId = null,
            )
        )

        sqlSlot.captured.contains("FROM log l") shouldBe true
        sqlSlot.captured.contains("ORDER BY l.timestamp DESC, l.id DESC") shouldBe true
        paramsSlot.captured.getValue("limit") shouldBe 101L
        result.size shouldBe 100L
        result.hasNext shouldBe false
        result.nextCursor shouldBe null
    }

    @Test
    fun `logLevel type traceId cursor가 있으면 where 조건을 동적으로 생성한다`() {
        val jdbc = mockk<NamedParameterJdbcTemplate>()
        val service = AdminLogQueryService(jdbc)

        val sqlSlot = slot<String>()
        val paramsSlot = slot<MapSqlParameterSource>()
        val cursorTs = LocalDateTime.of(2026, 3, 12, 21, 0, 0)

        every {
            jdbc.query(capture(sqlSlot), capture(paramsSlot), any<DataClassRowMapper<AdminLogSummary>>())
        } returns emptyList()

        service.findByConditions(
            AdminLogQueryConditions(
                logLevel = "INFO",
                type = "API",
                traceId = "abc",
                size = 50L,
                cursorTimestamp = cursorTs,
                cursorId = 123L,
            )
        )

        sqlSlot.captured.contains("l.level = :logLevel") shouldBe true
        sqlSlot.captured.contains("l.type = :type") shouldBe true
        sqlSlot.captured.contains("l.trace_id LIKE :traceId") shouldBe true
        sqlSlot.captured.contains("(l.timestamp < :cursorTimestamp OR (l.timestamp = :cursorTimestamp AND l.id < :cursorId))") shouldBe true
        paramsSlot.captured.getValue("logLevel") shouldBe "INFO"
        paramsSlot.captured.getValue("type") shouldBe "API"
        paramsSlot.captured.getValue("traceId") shouldBe "%abc%"
        paramsSlot.captured.getValue("cursorTimestamp") shouldBe cursorTs
        paramsSlot.captured.getValue("cursorId") shouldBe 123L
        paramsSlot.captured.getValue("limit") shouldBe 51L
    }

    @Test
    fun `size 초과 조회 시 hasNext와 nextCursor를 반환한다`() {
        val jdbc = mockk<NamedParameterJdbcTemplate>()
        val service = AdminLogQueryService(jdbc)
        val now = LocalDateTime.of(2026, 3, 12, 22, 0, 0)

        every {
            jdbc.query(any<String>(), any<MapSqlParameterSource>(), any<DataClassRowMapper<AdminLogSummary>>())
        } returns listOf(
            createRow(id = 30L, timestamp = now),
            createRow(id = 29L, timestamp = now.minusSeconds(1)),
            createRow(id = 28L, timestamp = now.minusSeconds(2)),
        )

        val result = service.findByConditions(
            AdminLogQueryConditions(
                logLevel = null,
                type = null,
                traceId = null,
                size = 2L,
                cursorTimestamp = null,
                cursorId = null,
            )
        )

        result.items.size shouldBe 2
        result.hasNext shouldBe true
        result.nextCursor shouldBe AdminLogCursor(timestamp = now.minusSeconds(1), id = 29L)
    }

    @Test
    fun `size는 최대 100으로 제한한다`() {
        val jdbc = mockk<NamedParameterJdbcTemplate>()
        val service = AdminLogQueryService(jdbc)
        val paramsSlot = slot<MapSqlParameterSource>()

        every {
            jdbc.query(any<String>(), capture(paramsSlot), any<DataClassRowMapper<AdminLogSummary>>())
        } returns emptyList()

        val result = service.findByConditions(
            AdminLogQueryConditions(
                logLevel = null,
                type = null,
                traceId = null,
                size = 500L,
                cursorTimestamp = null,
                cursorId = null,
            )
        )

        result.size shouldBe 100L
        paramsSlot.captured.getValue("limit") shouldBe 101L
    }

    private fun createRow(id: Long, timestamp: LocalDateTime): AdminLogSummary = AdminLogSummary(
        id = id,
        timestamp = timestamp,
        logLevel = "INFO",
        traceId = "trace-$id",
        loggerName = "logger",
        message = "message",
        type = "API",
        data = "{}",
    )
}

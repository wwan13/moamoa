package server.admin.feature.log.batch

import io.kotest.matchers.longs.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import test.UnitTest
import java.time.Duration
import java.time.LocalDateTime

class AdminLogCleanupProcessorTest : UnitTest() {

    @Test
    fun `7일 이상 지난 로그를 삭제한다`() {
        val jdbc = mockk<NamedParameterJdbcTemplate>()
        val service = AdminLogCleanupProcessor(jdbc)
        val sqlSlot = slot<String>()
        val paramsSlot = slot<MapSqlParameterSource>()
        val beforeCall = LocalDateTime.now()

        every { jdbc.update(capture(sqlSlot), capture(paramsSlot)) } returns 12

        val deletedCount = service.deleteLogsOlderThan()
        val afterCall = LocalDateTime.now()

        deletedCount shouldBe 12
        sqlSlot.captured shouldContain "DELETE FROM log"
        sqlSlot.captured shouldContain "timestamp < :cutoffTimestamp"

        val cutoffTimestamp = paramsSlot.captured.getValue("cutoffTimestamp") as LocalDateTime
        val lowerBound = beforeCall.minusDays(7)
        val upperBound = afterCall.minusDays(7)
        Duration.between(lowerBound, cutoffTimestamp).toMillis() shouldBeGreaterThanOrEqual 0
        Duration.between(cutoffTimestamp, upperBound).toMillis() shouldBeGreaterThanOrEqual 0
    }
}

package server.admin.feature.log.batch

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import test.UnitTest

class AdminLogCleanupSchedulerTest : UnitTest() {

    @Test
    fun `스케줄러가 로그 정리 서비스를 호출한다`() {
        val cleanupService = mockk<AdminLogCleanupProcessor>()
        val scheduler = AdminLogCleanupScheduler(cleanupService)

        every { cleanupService.deleteLogsOlderThan() } returns 5

        scheduler.cleanupOldLogs()

        verify(exactly = 1) { cleanupService.deleteLogsOlderThan() }
    }
}

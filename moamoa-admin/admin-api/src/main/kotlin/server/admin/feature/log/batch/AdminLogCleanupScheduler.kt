package server.admin.feature.log.batch

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
internal class AdminLogCleanupScheduler(
    private val adminLogCleanupProcessor: AdminLogCleanupProcessor,
) {

    @Scheduled(cron = "0 0 12 * * *", zone = "Asia/Seoul")
    fun cleanupOldLogs() {
        adminLogCleanupProcessor.deleteLogsOlderThan()
    }
}

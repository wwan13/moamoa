package server.batch.techblog.syncsubscriptioncount.scheduler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import server.batch.techblog.syncsubscriptioncount.job.SyncSubscriptionCountCoroutineJob

@Component
internal class SyncSubscriptionCountScheduler(
    private val batchScope: CoroutineScope,
    private val syncSubscriptionCountCoroutineJob: SyncSubscriptionCountCoroutineJob,
) {

    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Seoul")
    fun launchSyncSubscriptionCountJob() = batchScope.launch {
        syncSubscriptionCountCoroutineJob.run(mapOf("run.id" to System.currentTimeMillis().toString()))
    }
}

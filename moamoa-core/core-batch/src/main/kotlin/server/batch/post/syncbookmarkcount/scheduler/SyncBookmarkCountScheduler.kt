package server.batch.post.syncbookmarkcount.scheduler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import server.batch.post.syncbookmarkcount.SyncBookmarkCountCoroutineJob

@Component
internal class SyncBookmarkCountScheduler(
    private val batchScope: CoroutineScope,
    private val syncBookmarkCountCoroutineJob: SyncBookmarkCountCoroutineJob,
) {

    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Seoul")
    fun launchSyncBookmarkCountJob() = batchScope.launch {
        syncBookmarkCountCoroutineJob.run(mapOf("run.id" to System.currentTimeMillis().toString()))
    }
}

package server.batch.techblog.scheduler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import server.batch.common.queue.BatchQueue

@Component
internal class TechBlogScheduler(
    private val batchScope: CoroutineScope,
    private val batchQueue: BatchQueue,
    private val syncTechBlogSubscriptionCountJob: Job,
    private val collectTechBlogPostJob: Job,
    private val notifyTechBlogCollectResultJob: Job,
) {

    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Seoul")
    fun launchSyncTechBlogSubscriptionCountJob() = batchScope.launch {
        val params = JobParametersBuilder()
            .addLong("run.id", System.currentTimeMillis())
            .toJobParameters()
        batchQueue.enqueue(syncTechBlogSubscriptionCountJob, params)
    }

    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    fun launchCollectTechBlogPostJob() = batchScope.launch {
        val params = JobParametersBuilder()
            .addLong("run.id", System.currentTimeMillis())
            .addLong("postLimit", 10L)
            .toJobParameters()
        batchQueue.enqueue(collectTechBlogPostJob, params)
    }

    @Scheduled(cron = "0 0 10 * * *", zone = "Asia/Seoul")
    fun launchNotifyTechBlogCollectResultJob() = batchScope.launch {
        val params = JobParametersBuilder()
            .addLong("run.id", System.currentTimeMillis())
            .toJobParameters()
        batchQueue.enqueue(notifyTechBlogCollectResultJob, params)
    }
}

package server.techblog.scheduler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import server.common.queue.BatchQueue

@Component
class TechBlogScheduler(
    private val batchScope: CoroutineScope,
    private val batchQueue: BatchQueue,
    private val syncTechBlogSubscriptionCountJob: Job,
    private val fetchTechBlogPostsJob: Job,
) {

    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Seoul")
    fun launchSyncTechBlogSubscriptionCountJob() = batchScope.launch {
        val params = JobParametersBuilder()
            .addLong("run.id", System.currentTimeMillis())
            .toJobParameters()
        batchQueue.enqueue(syncTechBlogSubscriptionCountJob, params)
    }

    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    fun launchFetchTechBlogPostsJob() = batchScope.launch {
        val params = JobParametersBuilder()
            .addLong("run.id", System.currentTimeMillis())
            .toJobParameters()
        batchQueue.enqueue(fetchTechBlogPostsJob, params)
    }
}
package server.batch.post.scheduler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import server.batch.common.queue.BatchQueue

@Component
internal class PostScheduler(
    private val batchScope: CoroutineScope,
    private val batchQueue: BatchQueue,
    private val syncPostBookmarkCountJob: Job,
    private val updatePostViewCountJob: Job,
) {

    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Seoul")
    fun launchSyncPostBookmarkCountJob() = batchScope.launch {
        val params = JobParametersBuilder()
            .addLong("run.id", System.currentTimeMillis())
            .toJobParameters()
        batchQueue.enqueue(syncPostBookmarkCountJob, params)
    }

    @Scheduled(cron = "0 */1 * * * *", zone = "Asia/Seoul")
    fun launchUpdatePostViewCountJob() = batchScope.launch {
        val params = JobParametersBuilder()
            .addLong("run.id", System.currentTimeMillis())
            .toJobParameters()
        batchQueue.enqueue(updatePostViewCountJob, params)
    }

//    fun launchCategorizingPostJob() = batchScope.launch {
//        val params = JobParametersBuilder()
//            .addLong("run.id", System.currentTimeMillis())
//            .toJobParameters()
//        batchQueue.enqueue(syncPostBookmarkCountJob, params)
//    }
}